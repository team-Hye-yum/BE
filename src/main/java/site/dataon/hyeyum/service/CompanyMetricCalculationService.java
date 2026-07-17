package site.dataon.hyeyum.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import site.dataon.hyeyum.dto.CompanyMetricError;
import site.dataon.hyeyum.dto.CompanyMetricRecalculationResult;

@Service
public class CompanyMetricCalculationService {

    private static final Logger log = LoggerFactory.getLogger(CompanyMetricCalculationService.class);
    private static final int CAGR_START_YEAR = 2020;
    private static final int CAGR_END_YEAR = 2024;
    private static final String SUPPORTED_SELECTION_RESULT = "지원대상";

    private final JdbcTemplate jdbcTemplate;
    private final OpenAiCompanyMetricTextClient openAiCompanyMetricTextClient;
    private final int aiParallelism;

    public CompanyMetricCalculationService(
            JdbcTemplate jdbcTemplate,
            OpenAiCompanyMetricTextClient openAiCompanyMetricTextClient,
            @Value("${openai.company-metrics.parallelism:3}") int aiParallelism) {
        this.jdbcTemplate = jdbcTemplate;
        this.openAiCompanyMetricTextClient = openAiCompanyMetricTextClient;
        this.aiParallelism = Math.max(1, aiParallelism);
    }

    public CompanyMetricRecalculationResult recalculateAll() {
        updateIndustryDescriptionsFromKsicInfo();

        List<Integer> companyIds = jdbcTemplate.queryForList(
                "select company_id from company order by company_id",
                Integer.class);
        Map<Integer, FinancialRow> latestFinancials = latestFinancials();
        Map<Integer, EmploymentRow> latestEmployments = latestEmployments();
        Map<Integer, Double> salesGrowthRates = fixedYearFinancialCagrs();
        Map<Integer, Double> employmentGrowthRates = fixedYearEmploymentCagrs();
        Map<Integer, Double> governmentRndDependencies = governmentRndDependencies();
        Map<Integer, Double> supportedSalesGrowthRates = averageSupportedSalesGrowthRates();
        Map<Integer, CompanyProfile> companyProfiles = companyProfiles();

        List<CompanyMetricError> errors = Collections.synchronizedList(new ArrayList<>());
        List<CompanyMetricPendingUpdate> pendingUpdates = new ArrayList<>();
        for (Integer companyId : companyIds) {
            try {
                CompanyMetrics metrics =
                        calculate(
                                latestFinancials.get(companyId),
                                latestEmployments.get(companyId),
                                salesGrowthRates.get(companyId),
                                employmentGrowthRates.get(companyId),
                                governmentRndDependencies.get(companyId),
                                supportedSalesGrowthRates.get(companyId));
                pendingUpdates.add(new CompanyMetricPendingUpdate(
                        companyId,
                        metrics));
            } catch (RuntimeException exception) {
                log.warn("Failed to calculate company metrics. companyId={}", companyId, exception);
                errors.add(new CompanyMetricError(companyId, rootMessage(exception)));
            }
        }

        List<CompanyMetricUpdate> updates = generateAiTexts(pendingUpdates, companyProfiles, errors);
        CompanyMetricUpdateResult updateResult = batchUpdate(updates, errors);
        return new CompanyMetricRecalculationResult(
                companyIds.size(),
                updateResult.updatedCompanyCount(),
                updateResult.aiUpdatedCompanyCount(),
                errors.size(),
                List.copyOf(errors));
    }

    private int updateIndustryDescriptionsFromKsicInfo() {
        int updatedCount = jdbcTemplate.update(
                """
                update company
                set industry_description = ksic_info.industry_description
                from ksic_info
                where company.ksic_code = ksic_info.ksic_code
                """);
        log.info("Updated company industry descriptions from ksic_info. updatedCount={}", updatedCount);
        return updatedCount;
    }

    private CompanyMetrics calculate(
            FinancialRow latestFinancial,
            EmploymentRow latestEmployment,
            Double salesGrowthRate,
            Double employmentGrowthRate,
            Double governmentRndDependency,
            Double supportedSalesGrowthRate) {
        return new CompanyMetrics(
                debtRatio(latestFinancial),
                costOfSalesRatio(latestFinancial),
                salesGrowthRate,
                employmentGrowthRate,
                governmentRndDependency,
                supportedSalesGrowthRate,
                employmentPeakIndex(latestEmployment),
                employeeTurnoverRate(latestEmployment));
    }

    private Map<Integer, FinancialRow> latestFinancials() {
        return jdbcTemplate.query(
                """
                select distinct on (company_id)
                    company_id, year, sales_amount, cost_of_sales, total_liabilities, total_equity
                from company_financial_statistics
                order by company_id, year desc
                """,
                rs -> {
                    Map<Integer, FinancialRow> rows = new HashMap<>();
                    while (rs.next()) {
                        rows.put(
                                rs.getInt("company_id"),
                                new FinancialRow(
                                        integerOrNull(rs.getObject("year")),
                                        integerOrNull(rs.getObject("sales_amount")),
                                        integerOrNull(rs.getObject("cost_of_sales")),
                                        integerOrNull(rs.getObject("total_liabilities")),
                                        integerOrNull(rs.getObject("total_equity"))));
                    }
                    return rows;
                });
    }

    private Map<Integer, EmploymentRow> latestEmployments() {
        return jdbcTemplate.query(
                """
                select distinct on (company_id)
                    company_id, year, employee_count, pension_subscriber_count,
                    pension_new_hire_count, pension_retiree_count
                from company_employment_statistics
                order by company_id, year desc
                """,
                rs -> {
                    Map<Integer, EmploymentRow> rows = new HashMap<>();
                    while (rs.next()) {
                        rows.put(
                                rs.getInt("company_id"),
                                new EmploymentRow(
                                        integerOrNull(rs.getObject("year")),
                                        integerOrNull(rs.getObject("employee_count")),
                                        integerOrNull(rs.getObject("pension_subscriber_count")),
                                        integerOrNull(rs.getObject("pension_new_hire_count")),
                                        integerOrNull(rs.getObject("pension_retiree_count"))));
                    }
                    return rows;
                });
    }

    private Map<Integer, CompanyProfile> companyProfiles() {
        return jdbcTemplate.query(
                """
                select company_id, company_name, region_name, established_date, business_entity_type,
                       company_size, listing_status, company_type, ksic_code, industry_name,
                       industry_description, main_product, is_closed, company_status,
                       is_innobiz, is_mainbiz, is_venture_company, is_materials_company,
                       is_net_certified, is_nep_certified, researcher_count, has_research_lab,
                       has_rnd_department
                from company
                """,
                rs -> {
                    Map<Integer, CompanyProfile> rows = new HashMap<>();
                    while (rs.next()) {
                        rows.put(
                                rs.getInt("company_id"),
                                new CompanyProfile(
                                        rs.getInt("company_id"),
                                        rs.getString("company_name"),
                                        rs.getString("region_name"),
                                        rs.getObject("established_date", LocalDate.class),
                                        rs.getString("business_entity_type"),
                                        rs.getString("company_size"),
                                        rs.getString("listing_status"),
                                        rs.getString("company_type"),
                                        rs.getString("ksic_code"),
                                        rs.getString("industry_name"),
                                        rs.getString("industry_description"),
                                        rs.getString("main_product"),
                                        booleanOrNull(rs.getObject("is_closed")),
                                        rs.getString("company_status"),
                                        booleanOrNull(rs.getObject("is_innobiz")),
                                        booleanOrNull(rs.getObject("is_mainbiz")),
                                        booleanOrNull(rs.getObject("is_venture_company")),
                                        booleanOrNull(rs.getObject("is_materials_company")),
                                        booleanOrNull(rs.getObject("is_net_certified")),
                                        booleanOrNull(rs.getObject("is_nep_certified")),
                                        integerOrNull(rs.getObject("researcher_count")),
                                        booleanOrNull(rs.getObject("has_research_lab")),
                                        booleanOrNull(rs.getObject("has_rnd_department"))));
                    }
                    return rows;
                });
    }

    private Map<Integer, Double> fixedYearFinancialCagrs() {
        return fixedYearCagrs(
                """
                select start_stat.company_id,
                       start_stat.sales_amount as start_value,
                       end_stat.sales_amount as end_value
                from company_financial_statistics start_stat
                join company_financial_statistics end_stat
                  on end_stat.company_id = start_stat.company_id
                 and end_stat.year = ?
                where start_stat.year = ?
                """);
    }

    private Map<Integer, Double> fixedYearEmploymentCagrs() {
        return fixedYearCagrs(
                """
                select start_stat.company_id,
                       start_stat.employee_count as start_value,
                       end_stat.employee_count as end_value
                from company_employment_statistics start_stat
                join company_employment_statistics end_stat
                  on end_stat.company_id = start_stat.company_id
                 and end_stat.year = ?
                where start_stat.year = ?
                """);
    }

    private Map<Integer, Double> fixedYearCagrs(String sql) {
        return jdbcTemplate.query(
                sql,
                rs -> {
                    Map<Integer, Double> values = new HashMap<>();
                    while (rs.next()) {
                        values.put(
                                rs.getInt("company_id"),
                                cagr(
                                        doubleOrNull(rs.getObject("start_value")),
                                        doubleOrNull(rs.getObject("end_value")),
                                        CAGR_END_YEAR - CAGR_START_YEAR));
                    }
                    return values;
                },
                CAGR_END_YEAR,
                CAGR_START_YEAR);
    }

    private Map<Integer, Double> governmentRndDependencies() {
        return jdbcTemplate.query(
                """
                select company_id,
                       sum(coalesce(government_research_fund, 0)) as government_fund,
                       sum(coalesce(private_research_fund, 0)) as private_fund
                from company_ntis_lead_project
                group by company_id
                """,
                rs -> {
                    Map<Integer, Double> values = new HashMap<>();
                    while (rs.next()) {
                        Double governmentFund = doubleOrNull(rs.getObject("government_fund"));
                        Double privateFund = doubleOrNull(rs.getObject("private_fund"));
                        double denominator = nullToZero(governmentFund) + nullToZero(privateFund);
                        values.put(
                                rs.getInt("company_id"),
                                denominator == 0.0 ? null : nullToZero(governmentFund) / denominator * 100.0);
                    }
                    return values;
                });
    }

    private Map<Integer, Double> averageSupportedSalesGrowthRates() {
        return jdbcTemplate.query(
                """
                select support.company_id,
                       avg((post.sales_amount - pre.sales_amount)::double precision / pre.sales_amount * 100.0) as growth_rate
                from (
                    select distinct company_id, support_year
                    from btp_support_history
                    where support_year is not null
                      and selection_result = ?
                ) support
                join company_financial_statistics pre
                  on pre.company_id = support.company_id and pre.year = support.support_year
                join company_financial_statistics post
                  on post.company_id = support.company_id and post.year = support.support_year + 1
                where pre.sales_amount is not null
                  and pre.sales_amount > 0
                  and post.sales_amount is not null
                group by support.company_id
                """,
                rs -> {
                    Map<Integer, Double> values = new HashMap<>();
                    while (rs.next()) {
                        values.put(rs.getInt("company_id"), doubleOrNull(rs.getObject("growth_rate")));
                    }
                    return values;
                },
                SUPPORTED_SELECTION_RESULT);
    }

    private Double debtRatio(FinancialRow financial) {
        if (financial == null) {
            return null;
        }
        return percentage(financial.totalLiabilities(), financial.totalEquity());
    }

    private Double costOfSalesRatio(FinancialRow financial) {
        if (financial == null) {
            return null;
        }
        return percentage(financial.costOfSales(), financial.salesAmount());
    }

    private Double employmentPeakIndex(EmploymentRow employment) {
        if (employment == null || employment.employeeCount() == null || employment.pensionSubscriberCount() == null) {
            return null;
        }
        if (employment.employeeCount() == 0) {
            return null;
        }
        return Math.abs(employment.employeeCount() - employment.pensionSubscriberCount())
                / (double) employment.employeeCount()
                * 100.0;
    }

    private Double employeeTurnoverRate(EmploymentRow employment) {
        if (employment == null) {
            return null;
        }
        return ratio(employment.pensionRetireeCount(), employment.pensionNewHireCount());
    }

    private Double percentage(Integer numerator, Integer denominator) {
        Double ratio = ratio(numerator, denominator);
        return ratio == null ? null : ratio * 100.0;
    }

    private Double ratio(Integer numerator, Integer denominator) {
        if (numerator == null || denominator == null || denominator == 0) {
            return null;
        }
        return numerator / (double) denominator;
    }

    private Double cagr(Double startValue, Double endValue, int yearDiff) {
        if (startValue == null || endValue == null || yearDiff <= 0 || startValue <= 0.0 || endValue <= 0.0) {
            return null;
        }
        return (Math.pow(endValue / startValue, 1.0 / yearDiff) - 1.0) * 100.0;
    }

    private List<CompanyMetricUpdate> generateAiTexts(
            List<CompanyMetricPendingUpdate> pendingUpdates,
            Map<Integer, CompanyProfile> companyProfiles,
            List<CompanyMetricError> errors) {
        if (aiParallelism == 1 || pendingUpdates.size() <= 1) {
            List<CompanyMetricUpdate> updates = new ArrayList<>();
            for (CompanyMetricPendingUpdate pendingUpdate : pendingUpdates) {
                updates.add(withAiText(pendingUpdate, companyProfiles.get(pendingUpdate.companyId()), errors));
            }
            return updates;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(aiParallelism);
        try {
            List<CompletableFuture<CompanyMetricUpdate>> futures = pendingUpdates.stream()
                    .map(pendingUpdate -> CompletableFuture.supplyAsync(
                            () -> withAiText(pendingUpdate, companyProfiles.get(pendingUpdate.companyId()), errors),
                            executorService))
                    .toList();
            List<CompanyMetricUpdate> updates = new ArrayList<>();
            for (CompletableFuture<CompanyMetricUpdate> future : futures) {
                updates.add(future.join());
            }
            return updates;
        } finally {
            executorService.shutdown();
        }
    }

    private CompanyMetricUpdate withAiText(
            CompanyMetricPendingUpdate pendingUpdate,
            CompanyProfile profile,
            List<CompanyMetricError> errors) {
        CompanyMetricAiText aiText = generateAiText(
                pendingUpdate.companyId(),
                profile,
                pendingUpdate.metrics(),
                errors);
        return new CompanyMetricUpdate(
                pendingUpdate.companyId(),
                pendingUpdate.metrics(),
                aiText);
    }

    private CompanyMetricAiText generateAiText(
            Integer companyId,
            CompanyProfile profile,
            CompanyMetrics metrics,
            List<CompanyMetricError> errors) {
        try {
            CompanyMetricAiRequest request = toAiRequest(companyId, profile, metrics);
            return openAiCompanyMetricTextClient.generate(request);
        } catch (RuntimeException exception) {
            log.warn("Failed to generate company metric AI text. companyId={}", companyId, exception);
            errors.add(new CompanyMetricError(companyId, "AI text generation failed: " + rootMessage(exception)));
            return null;
        }
    }

    private CompanyMetricAiRequest toAiRequest(
            Integer companyId,
            CompanyProfile profile,
            CompanyMetrics metrics) {
        return new CompanyMetricAiRequest(
                companyId,
                profile == null ? null : profile.companyName(),
                profile == null ? null : profile.regionName(),
                profile == null ? null : profile.establishedDate(),
                profile == null ? null : profile.businessEntityType(),
                profile == null ? null : profile.companySize(),
                profile == null ? null : profile.listingStatus(),
                profile == null ? null : profile.companyType(),
                profile == null ? null : profile.ksicCode(),
                profile == null ? null : profile.industryName(),
                profile == null ? null : profile.industryDescription(),
                profile == null ? null : profile.mainProduct(),
                profile == null ? null : profile.isClosed(),
                profile == null ? null : profile.companyStatus(),
                profile == null ? null : profile.isInnobiz(),
                profile == null ? null : profile.isMainbiz(),
                profile == null ? null : profile.isVentureCompany(),
                profile == null ? null : profile.isMaterialsCompany(),
                profile == null ? null : profile.isNetCertified(),
                profile == null ? null : profile.isNepCertified(),
                profile == null ? null : profile.researcherCount(),
                profile == null ? null : profile.hasResearchLab(),
                profile == null ? null : profile.hasRndDepartment(),
                metrics.debtRatio(),
                metrics.costOfSalesRatio(),
                metrics.salesGrowthRate(),
                metrics.employmentGrowthRate(),
                metrics.governmentRndDependency(),
                metrics.supportedSalesGrowthRate(),
                metrics.employmentPeakIndex(),
                metrics.employeeTurnoverRate());
    }

    private CompanyMetricUpdateResult batchUpdate(List<CompanyMetricUpdate> updates, List<CompanyMetricError> errors) {
        if (updates.isEmpty()) {
            return new CompanyMetricUpdateResult(0, 0);
        }
        try {
            int[] results = jdbcTemplate.batchUpdate(
                    """
                    update company
                    set debt_ratio = ?,
                        cost_of_sales_ratio = ?,
                        sales_growth_rate = ?,
                        employment_growth_rate = ?,
                        government_rnd_dependency = ?,
                        supported_sales_growth_rate = ?,
                        employment_peak_index = ?,
                        employee_turnover_rate = ?,
                        ai_summary = coalesce(?, ai_summary),
                        ai_one_line_summary = coalesce(?, ai_one_line_summary)
                    where company_id = ?
                    """,
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            CompanyMetricUpdate update = updates.get(i);
                            CompanyMetrics metrics = update.metrics();
                            setDoubleOrNull(ps, 1, metrics.debtRatio());
                            setDoubleOrNull(ps, 2, metrics.costOfSalesRatio());
                            setDoubleOrNull(ps, 3, metrics.salesGrowthRate());
                            setDoubleOrNull(ps, 4, metrics.employmentGrowthRate());
                            setDoubleOrNull(ps, 5, metrics.governmentRndDependency());
                            setDoubleOrNull(ps, 6, metrics.supportedSalesGrowthRate());
                            setDoubleOrNull(ps, 7, metrics.employmentPeakIndex());
                            setDoubleOrNull(ps, 8, metrics.employeeTurnoverRate());
                            CompanyMetricAiText aiText = update.aiText();
                            ps.setString(9, aiText == null ? null : aiText.aiSummary());
                            ps.setString(10, aiText == null ? null : aiText.aiOneLineSummary());
                            ps.setInt(11, update.companyId());
                        }

                        @Override
                        public int getBatchSize() {
                            return updates.size();
                        }
                    });
            int updatedCount = 0;
            for (int result : results) {
                if (result >= 0) {
                    updatedCount += result;
                } else if (result == Statement.SUCCESS_NO_INFO) {
                    updatedCount++;
                }
            }
            int aiUpdatedCount = 0;
            for (CompanyMetricUpdate update : updates) {
                if (update.aiText() != null) {
                    aiUpdatedCount++;
                }
            }
            return new CompanyMetricUpdateResult(updatedCount, aiUpdatedCount);
        } catch (RuntimeException exception) {
            log.warn("Batch update for company metrics failed. Falling back to per-company updates.", exception);
            return updateOneByOne(updates, errors);
        }
    }

    private CompanyMetricUpdateResult updateOneByOne(List<CompanyMetricUpdate> updates, List<CompanyMetricError> errors) {
        int updatedCount = 0;
        int aiUpdatedCount = 0;
        for (CompanyMetricUpdate update : updates) {
            try {
                int count = updateCompany(update);
                updatedCount += count;
                if (count > 0 && update.aiText() != null) {
                    aiUpdatedCount++;
                }
            } catch (RuntimeException exception) {
                log.warn("Failed to update company metrics. companyId={}", update.companyId(), exception);
                errors.add(new CompanyMetricError(update.companyId(), rootMessage(exception)));
            }
        }
        return new CompanyMetricUpdateResult(updatedCount, aiUpdatedCount);
    }

    private int updateCompany(CompanyMetricUpdate update) {
        CompanyMetrics metrics = update.metrics();
        return jdbcTemplate.update(
                """
                update company
                set debt_ratio = ?,
                    cost_of_sales_ratio = ?,
                    sales_growth_rate = ?,
                    employment_growth_rate = ?,
                    government_rnd_dependency = ?,
                    supported_sales_growth_rate = ?,
                    employment_peak_index = ?,
                    employee_turnover_rate = ?,
                    ai_summary = coalesce(?, ai_summary),
                    ai_one_line_summary = coalesce(?, ai_one_line_summary)
                where company_id = ?
                """,
                metrics.debtRatio(),
                metrics.costOfSalesRatio(),
                metrics.salesGrowthRate(),
                metrics.employmentGrowthRate(),
                metrics.governmentRndDependency(),
                metrics.supportedSalesGrowthRate(),
                metrics.employmentPeakIndex(),
                metrics.employeeTurnoverRate(),
                update.aiText() == null ? null : update.aiText().aiSummary(),
                update.aiText() == null ? null : update.aiText().aiOneLineSummary(),
                update.companyId());
    }

    private void setDoubleOrNull(PreparedStatement ps, int parameterIndex, Double value) throws SQLException {
        if (value == null) {
            ps.setObject(parameterIndex, null);
            return;
        }
        ps.setDouble(parameterIndex, value);
    }

    private Integer integerOrNull(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private Double doubleOrNull(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }

    private Boolean booleanOrNull(Object value) {
        return value == null ? null : (Boolean) value;
    }

    private double nullToZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private record FinancialRow(
            Integer year,
            Integer salesAmount,
            Integer costOfSales,
            Integer totalLiabilities,
            Integer totalEquity) {}

    private record EmploymentRow(
            Integer year,
            Integer employeeCount,
            Integer pensionSubscriberCount,
            Integer pensionNewHireCount,
            Integer pensionRetireeCount) {}

    private record CompanyProfile(
            Integer companyId,
            String companyName,
            String regionName,
            LocalDate establishedDate,
            String businessEntityType,
            String companySize,
            String listingStatus,
            String companyType,
            String ksicCode,
            String industryName,
            String industryDescription,
            String mainProduct,
            Boolean isClosed,
            String companyStatus,
            Boolean isInnobiz,
            Boolean isMainbiz,
            Boolean isVentureCompany,
            Boolean isMaterialsCompany,
            Boolean isNetCertified,
            Boolean isNepCertified,
            Integer researcherCount,
            Boolean hasResearchLab,
            Boolean hasRndDepartment) {}

    private record CompanyMetricPendingUpdate(Integer companyId, CompanyMetrics metrics) {}

    private record CompanyMetricUpdate(Integer companyId, CompanyMetrics metrics, CompanyMetricAiText aiText) {}

    private record CompanyMetricUpdateResult(int updatedCompanyCount, int aiUpdatedCompanyCount) {}

    private record CompanyMetrics(
            Double debtRatio,
            Double costOfSalesRatio,
            Double salesGrowthRate,
            Double employmentGrowthRate,
            Double governmentRndDependency,
            Double supportedSalesGrowthRate,
            Double employmentPeakIndex,
            Double employeeTurnoverRate) {}
}
