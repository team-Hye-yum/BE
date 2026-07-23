package site.dataon.hyeyum.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import site.dataon.hyeyum.common.MetricUnits;
import site.dataon.hyeyum.common.MoneyUnits;
import site.dataon.hyeyum.common.PatentRegistrationStatuses;
import site.dataon.hyeyum.domain.BtpSupportHistory;
import site.dataon.hyeyum.domain.Company;
import site.dataon.hyeyum.domain.CompanyEmploymentStatistics;
import site.dataon.hyeyum.domain.CompanyFinancialStatistics;
import site.dataon.hyeyum.domain.CompanyIndustryBenchmarkMapping;
import site.dataon.hyeyum.domain.CompanyNtisCollaborativeProject;
import site.dataon.hyeyum.domain.CompanyNtisLeadProject;
import site.dataon.hyeyum.domain.CompanyPatent;
import site.dataon.hyeyum.domain.IndustryBenchmarkIndex;
import site.dataon.hyeyum.domain.IndustryBenchmarkMetric;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.AiSummaryResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.CertificationBadge;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.CertificationsIpSummaryResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.CompanyExistenceResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.CompanyProfileResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ComputedMetricItem;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ComputedMetricsResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.DebtRatioDerived;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.EmploymentPoint;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.EmploymentsResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.FinancialPositionPoint;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.FinancialPositionResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.GrowthChartLine;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.GrowthPoint;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.GrowthScenarioResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.IncomeStatementDerived;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.IncomeStatementPoint;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.IncomeStatementsResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.MetricsAtMarker;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.MoneyValue;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisCollaborativeProjectItem;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisCollaborativeProjectListResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisLeadProjectItem;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisLeadProjectListResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisSummary;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ObservedFlow;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ObservedFlowDirection;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ObservedFlowPeriod;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ObservedFlowRow;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.PatentItem;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.PatentListResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ProductiveActivitiesSummaryResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ResearchOrganization;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ResearchDevelopmentStatusResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ResearchOrganizationStatus;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.SupportHistoriesSummary;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.SupportMarker;
import site.dataon.hyeyum.repository.BtpSupportHistoryRepository;
import site.dataon.hyeyum.repository.CompanyEmploymentStatisticsRepository;
import site.dataon.hyeyum.repository.CompanyFinancialStatisticsRepository;
import site.dataon.hyeyum.repository.CompanyIndustryBenchmarkMappingRepository;
import site.dataon.hyeyum.repository.CompanyNtisCollaborativeProjectRepository;
import site.dataon.hyeyum.repository.CompanyNtisLeadProjectRepository;
import site.dataon.hyeyum.repository.CompanyPatentRepository;
import site.dataon.hyeyum.repository.CompanyRepository;
import site.dataon.hyeyum.repository.IndustryBenchmarkIndexRepository;
import site.dataon.hyeyum.repository.IndustryBenchmarkMetricRepository;

@Service
public class CompanyDashboardService {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String REVENUE_INDEX = "REVENUE_INDEX";
    private static final String REVENUE_GROWTH_RATE = "REVENUE_GROWTH_RATE";
    private static final Integer BASE_YEAR = 2021;
    private static final Integer LAST_OBSERVED_YEAR = 2024;
    private static final Integer REFERENCE_YEAR = 2025;
    private static final Double STABILITY_SCALE_C = 40.0;
    private static final String DISCLAIMER =
            "지원사업이 기업 성장의 원인임을 의미하지 않으며, 성장 흐름을 참고하기 위한 정보입니다. 2025년은 한국은행 발표 자료를 기반으로 산출한 참고 경로이며 기업의 미래를 예측하지 않습니다.";

    private final CompanyRepository companyRepository;
    private final CompanyFinancialStatisticsRepository financialStatisticsRepository;
    private final CompanyEmploymentStatisticsRepository employmentStatisticsRepository;
    private final CompanyPatentRepository patentRepository;
    private final BtpSupportHistoryRepository supportHistoryRepository;
    private final CompanyNtisLeadProjectRepository ntisLeadProjectRepository;
    private final CompanyNtisCollaborativeProjectRepository ntisCollaborativeProjectRepository;
    private final CompanyIndustryBenchmarkMappingRepository benchmarkMappingRepository;
    private final IndustryBenchmarkIndexRepository benchmarkIndexRepository;
    private final IndustryBenchmarkMetricRepository benchmarkMetricRepository;
    public CompanyDashboardService(
            CompanyRepository companyRepository,
            CompanyFinancialStatisticsRepository financialStatisticsRepository,
            CompanyEmploymentStatisticsRepository employmentStatisticsRepository,
            CompanyPatentRepository patentRepository,
            BtpSupportHistoryRepository supportHistoryRepository,
            CompanyNtisLeadProjectRepository ntisLeadProjectRepository,
            CompanyNtisCollaborativeProjectRepository ntisCollaborativeProjectRepository,
            CompanyIndustryBenchmarkMappingRepository benchmarkMappingRepository,
            IndustryBenchmarkIndexRepository benchmarkIndexRepository,
            IndustryBenchmarkMetricRepository benchmarkMetricRepository) {
        this.companyRepository = companyRepository;
        this.financialStatisticsRepository = financialStatisticsRepository;
        this.employmentStatisticsRepository = employmentStatisticsRepository;
        this.patentRepository = patentRepository;
        this.supportHistoryRepository = supportHistoryRepository;
        this.ntisLeadProjectRepository = ntisLeadProjectRepository;
        this.ntisCollaborativeProjectRepository = ntisCollaborativeProjectRepository;
        this.benchmarkMappingRepository = benchmarkMappingRepository;
        this.benchmarkIndexRepository = benchmarkIndexRepository;
        this.benchmarkMetricRepository = benchmarkMetricRepository;
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<CompanyExistenceResponse> existence(Integer companyId) {
        return new ApiDataResponse<>(new CompanyExistenceResponse(companyId, companyRepository.existsById(companyId)));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<CompanyProfileResponse> profile(Integer companyId) {
        Company company = findCompany(companyId);
        return new ApiDataResponse<>(new CompanyProfileResponse(
                company.getCompanyId(),
                company.getCompanyName(),
                company.getRegionName(),
                company.getBusinessEntityType(),
                company.getCompanyType(),
                company.getListingStatus(),
                formatDate(company.getEstablishedDate()),
                formatDate(company.getReferenceDate()),
                companyAge(company.getEstablishedDate(), company.getReferenceDate()),
                company.getKsicCode(),
                company.getIndustryName(),
                company.getMainProduct(),
                company.getAddress()));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<FinancialPositionResponse> financialPosition(Integer companyId) {
        Company company = findCompany(companyId);
        List<FinancialPositionPoint> series = financialStatisticsRepository.findByCompanyIdOrderByYearAsc(companyId).stream()
                .map(statistics -> new FinancialPositionPoint(
                        statistics.getYear(),
                        statistics.getTotalAssets(),
                        statistics.getTotalLiabilities(),
                        statistics.getTotalEquity(),
                        statistics.getPaidInCapital(),
                        new DebtRatioDerived(round(company.getDebtRatio()))))
                .toList();
        return new ApiDataResponse<>(new FinancialPositionResponse(companyId, MoneyUnits.KRW_THOUSAND, series));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<IncomeStatementsResponse> incomeStatements(Integer companyId) {
        Company company = findCompany(companyId);
        List<CompanyFinancialStatistics> statistics = financialStatisticsRepository.findByCompanyIdOrderByYearAsc(companyId);
        Map<Integer, Integer> salesByYear = new HashMap<>();
        statistics.forEach(stat -> salesByYear.put(stat.getYear(), stat.getSalesAmount()));
        List<IncomeStatementPoint> series = statistics.stream()
                .map(stat -> new IncomeStatementPoint(
                        stat.getYear(),
                        stat.getSalesAmount(),
                        stat.getCostOfSales(),
                        stat.getOperatingIncome(),
                        stat.getNetIncome(),
                        round(stat.getOperatingMargin()),
                        stat.getResearchAndDevelopmentExpense(),
                        new IncomeStatementDerived(
                                growthPercent(salesByYear.get(stat.getYear() - 1), stat.getSalesAmount()),
                                round(company.getCostOfSalesRatio()))))
                .toList();
        return new ApiDataResponse<>(new IncomeStatementsResponse(companyId, MoneyUnits.KRW_THOUSAND, series));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<EmploymentsResponse> employments(Integer companyId) {
        findCompany(companyId);
        List<CompanyEmploymentStatistics> statistics = employmentStatisticsRepository.findByCompanyIdOrderByYearAsc(companyId);
        Map<Integer, Integer> employeeByYear = new HashMap<>();
        statistics.forEach(stat -> employeeByYear.put(stat.getYear(), stat.getEmployeeCount()));
        List<EmploymentPoint> series = statistics.stream()
                .map(stat -> {
                    Integer previous = employeeByYear.get(stat.getYear() - 1);
                    Integer current = stat.getEmployeeCount();
                    Integer change = previous == null || current == null ? null : current - previous;
                    return new EmploymentPoint(
                            stat.getYear(),
                            current,
                            stat.getPensionSubscriberCount(),
                            stat.getPensionNewHireCount(),
                            stat.getPensionRetireeCount(),
                            stat.getAverageSalary(),
                            change,
                            growthPercent(previous, current));
                })
                .toList();
        return new ApiDataResponse<>(new EmploymentsResponse(companyId, series));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<CertificationsIpSummaryResponse> certificationsIpSummary(Integer companyId) {
        Company company = findCompany(companyId);
        long activePatentCount = patentRepository.countByCompanyIdAndRegistrationStatusAndIsActiveTrue(
                companyId, PatentRegistrationStatuses.REGISTERED);
        return new ApiDataResponse<>(new CertificationsIpSummaryResponse(
                companyId,
                activePatentCount,
                List.of(
                        new CertificationBadge("VENTURE", "벤처기업", company.getIsVentureCompany()),
                        new CertificationBadge("INNOBIZ", "이노비즈", company.getIsInnobiz()),
                        new CertificationBadge("MAINBIZ", "메인비즈", company.getIsMainbiz()),
                        new CertificationBadge("MATERIALS_PARTS", "소재부품", company.getIsMaterialsCompany()),
                        new CertificationBadge("NET", "NET", company.getIsNetCertified()),
                        new CertificationBadge("NEP", "NEP", company.getIsNepCertified())),
                List.of(
                        new ResearchOrganization(
                                "RESEARCH_LAB",
                                "기업부설연구소",
                                company.getHasResearchLab(),
                                formatDate(company.getResearchLabRegisteredDate())),
                        new ResearchOrganization(
                                "RND_DEPARTMENT",
                                "연구개발전담부서",
                                company.getHasRndDepartment(),
                                formatDate(company.getRndDepartmentRegisteredDate())))));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<ResearchDevelopmentStatusResponse> researchDevelopmentStatus(Integer companyId) {
        Company company = findCompany(companyId);
        return new ApiDataResponse<>(new ResearchDevelopmentStatusResponse(
                company.getResearcherCount(),
                new ResearchOrganizationStatus(
                        company.getHasResearchLab(),
                        registeredDate(company.getHasResearchLab(), company.getResearchLabRegisteredDate())),
                new ResearchOrganizationStatus(
                        company.getHasRndDepartment(),
                        registeredDate(company.getHasRndDepartment(), company.getRndDepartmentRegisteredDate()))));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<PatentListResponse> patents(Integer companyId) {
        findCompany(companyId);
        List<PatentItem> items = patentRepository.findByCompanyIdOrderByRegistrationDateDescPatentIdAsc(companyId).stream()
                .map(patent -> new PatentItem(
                        patent.getPatentId(),
                        patent.getPatentType(),
                        patent.getRegistrationStatus(),
                        formatDate(patent.getApplicationDate()),
                        formatDate(patent.getRegistrationDate()),
                        patent.getCompanyRelationCode(),
                        patent.getIsActive()))
                .toList();
        return new ApiDataResponse<>(new PatentListResponse(items));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<ProductiveActivitiesSummaryResponse> productiveActivitiesSummary(Integer companyId) {
        findCompany(companyId);
        List<CompanyNtisLeadProject> leadProjects = ntisLeadProjectRepository
                .findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisLeadProjectIdAsc(companyId);
        List<CompanyNtisCollaborativeProject> collaborativeProjects = ntisCollaborativeProjectRepository
                .findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisCollaborativeProjectIdAsc(companyId);
        List<BtpSupportHistory> supportHistories = supportHistoryRepository.findByCompanyIdOrderBySupportYearAscSupportHistoryIdAsc(companyId);
        Integer latestParticipationYear = maxReferenceYear(leadProjects, collaborativeProjects);
        long recentFiveYearFund = leadProjects.stream()
                .filter(project -> latestParticipationYear == null
                        || (project.getReferenceYear() != null && project.getReferenceYear() >= latestParticipationYear - 4))
                .map(CompanyNtisLeadProject::getGovernmentResearchFund)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        Integer latestSupportYear = supportHistories.stream()
                .map(BtpSupportHistory::getSupportYear)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);

        NtisSummary ntis = new NtisSummary(
                leadProjects.size(),
                collaborativeProjects.size(),
                new MoneyValue(recentFiveYearFund, MoneyUnits.KRW),
                latestParticipationYear,
                "동일 과제, 동일 기준연도 조합에서 최신 기준일자만 표시");
        return new ApiDataResponse<>(new ProductiveActivitiesSummaryResponse(
                companyId, ntis, new SupportHistoriesSummary(supportHistories.size(), latestSupportYear)));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<NtisLeadProjectListResponse> ntisLeadProjects(Integer companyId) {
        findCompany(companyId);
        List<NtisLeadProjectItem> items = ntisLeadProjectRepository
                .findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisLeadProjectIdAsc(companyId)
                .stream()
                .map(project -> new NtisLeadProjectItem(
                        project.getNtisLeadProjectId(),
                        project.getReferenceYear(),
                        formatDate(project.getReferenceDate()),
                        project.getProjectName(),
                        project.getSupervisingMinistryName(),
                        project.getRegionName(),
                        formatDate(project.getTotalResearchStartDate()),
                        formatDate(project.getTotalResearchEndDate()),
                        formatDate(project.getAnnualResearchStartDate()),
                        formatDate(project.getAnnualResearchEndDate()),
                        project.getScienceTechnologyCategoryName(),
                        new MoneyValue(project.getGovernmentResearchFund(), MoneyUnits.KRW),
                        new MoneyValue(project.getPrivateResearchFund(), MoneyUnits.KRW),
                        new MoneyValue(project.getTotalResearchFund(), MoneyUnits.KRW)))
                .toList();
        return new ApiDataResponse<>(new NtisLeadProjectListResponse(items));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<NtisCollaborativeProjectListResponse> ntisCollaborativeProjects(Integer companyId) {
        findCompany(companyId);
        List<NtisCollaborativeProjectItem> items = ntisCollaborativeProjectRepository
                .findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisCollaborativeProjectIdAsc(companyId)
                .stream()
                .map(project -> new NtisCollaborativeProjectItem(
                        project.getNtisCollaborativeProjectId(),
                        project.getReferenceYear(),
                        formatDate(project.getReferenceDate()),
                        project.getResearchTypeName(),
                        project.getCollaborationParticipationTypeName(),
                        project.getCollaborationCountryName(),
                        project.getResearchPerformerTypeName(),
                        new MoneyValue(project.getCommissionedResearchFund(), MoneyUnits.KRW),
                        new MoneyValue(project.getCollaborativeResearchExpense(), MoneyUnits.KRW),
                        new MoneyValue(project.getCollaborativeResearchIncome(), MoneyUnits.KRW),
                        project.getHasCompanyCollaboration(),
                        project.getHasUniversityCollaboration(),
                        project.getHasPublicInstituteCollaboration(),
                        project.getHasForeignInstituteCollaboration(),
                        project.getHasOtherCollaboration()))
                .toList();
        return new ApiDataResponse<>(new NtisCollaborativeProjectListResponse(items));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<ComputedMetricsResponse> computedMetrics(Integer companyId) {
        Company company = findCompany(companyId);
        return new ApiDataResponse<>(new ComputedMetricsResponse(
                companyId,
                List.of(
                        metric("DEBT_RATIO", "부채 비율", company.getDebtRatio(), MetricUnits.PERCENT),
                        metric("COST_OF_SALES_RATIO", "매출 원가율", company.getCostOfSalesRatio(), MetricUnits.PERCENT),
                        metric("SALES_GROWTH", "매출 성장성", company.getSalesGrowthRate(), MetricUnits.PERCENT),
                        metric("EMPLOYMENT_GROWTH", "고용 성장성", company.getEmploymentGrowthRate(), MetricUnits.PERCENT),
                        metric("GOVERNMENT_RD_DEPENDENCY", "정부 R&D 의존도", company.getGovernmentRndDependency(), MetricUnits.PERCENT),
                        metric("SUPPORTED_COMPANY_SALES_CHANGE_RATE", "지원 기업 매출 변화율", company.getSupportedSalesGrowthRate(), MetricUnits.PERCENT),
                        metric("EMPLOYMENT_DIVERGENCE_INDEX", "고용 괴리 지수", company.getEmploymentPeakIndex(), MetricUnits.PERCENT_POINT),
                        metric("EMPLOYMENT_TURNOVER_RATE", "고용 회전율", company.getEmployeeTurnoverRate(), MetricUnits.PERCENT))));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<GrowthScenarioResponse> growthScenario(Integer companyId) {
        findCompany(companyId);
        List<CompanyFinancialStatistics> financials = financialStatisticsRepository.findByCompanyIdOrderByYearAsc(companyId);
        Map<Integer, Integer> salesByYear = new HashMap<>();
        Map<Integer, CompanyFinancialStatistics> financialsByYear = new HashMap<>();
        financials.stream()
                .filter(stat -> stat.getYear() != null)
                .forEach(stat -> {
                    salesByYear.put(stat.getYear(), stat.getSalesAmount());
                    financialsByYear.put(stat.getYear(), stat);
                });
        Map<Integer, Integer> employeesByYear = new HashMap<>();
        employmentStatisticsRepository.findByCompanyIdOrderByYearAsc(companyId).stream()
                .filter(stat -> stat.getYear() != null)
                .forEach(stat -> employeesByYear.put(stat.getYear(), stat.getEmployeeCount()));
        String bokIndustryCode = benchmarkMappingRepository
                .findById(companyId)
                .map(CompanyIndustryBenchmarkMapping::getBokIndustryCode)
                .orElse(null);
        Map<Integer, Double> companyIndexes = companyIndexByYear(salesByYear);
        Map<Integer, Double> companyGrowthRates = companyGrowthRateByYear(salesByYear);
        Map<Integer, Double> industryGrowthRates = industryGrowthRateByYear(bokIndustryCode);
        Map<Integer, Double> industryIndexes = industryIndexByYear(industryGrowthRates);

        Double industryReferenceGrowthRate2025 = industryReferenceGrowthRate2025(industryGrowthRates);
        Double industryReferenceIndex2025 = referenceIndex(industryIndexes.get(LAST_OBSERVED_YEAR), industryReferenceGrowthRate2025);
        Double firmReferenceGrowthRate2025 = industryReferenceGrowthRate2025 == null
                ? null
                : industryReferenceGrowthRate2025 + weightedCorrection(companyGrowthRates, industryGrowthRates);
        Double firmReferenceIndex2025 = referenceIndex(companyIndexes.get(LAST_OBSERVED_YEAR), firmReferenceGrowthRate2025);
        List<SupportMarker> markers = supportHistoryRepository.findByCompanyIdOrderBySupportYearAscSupportHistoryIdAsc(companyId).stream()
                .map(history -> supportMarker(history, financialsByYear, employeesByYear))
                .toList();
        return new ApiDataResponse<>(new GrowthScenarioResponse(
                companyId,
                chartLines(companyIndexes, firmReferenceIndex2025, industryIndexes, industryReferenceIndex2025),
                markers,
                observedFlow(financialsByYear, employeesByYear)));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<AiSummaryResponse> aiSummary(Integer companyId) {
        return new ApiDataResponse<>(new AiSummaryResponse(findCompany(companyId).getAiSummary()));
    }

    private Company findCompany(Integer companyId) {
        return companyRepository
                .findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "기업 정보를 찾을 수 없습니다."));
    }

    private List<GrowthChartLine> chartLines(
            Map<Integer, Double> companyIndexes,
            Double firmReferenceIndex2025,
            Map<Integer, Double> industryIndexes,
            Double industryReferenceIndex2025) {
        List<GrowthChartLine> lines = new ArrayList<>();
        lines.add(new GrowthChartLine("COMPANY_ACTUAL", actualPoints(companyIndexes)));
        if (companyIndexes.get(LAST_OBSERVED_YEAR) != null && firmReferenceIndex2025 != null) {
            lines.add(new GrowthChartLine(
                    "COMPANY_REFERENCE",
                    List.of(
                            new GrowthPoint(LAST_OBSERVED_YEAR, companyIndexes.get(LAST_OBSERVED_YEAR)),
                            new GrowthPoint(REFERENCE_YEAR, firmReferenceIndex2025))));
        }
        if (!industryIndexes.isEmpty()) {
            lines.add(new GrowthChartLine("INDUSTRY_ACTUAL", actualPoints(industryIndexes)));
        }
        if (industryIndexes.get(LAST_OBSERVED_YEAR) != null && industryReferenceIndex2025 != null) {
            lines.add(new GrowthChartLine(
                    "INDUSTRY_REFERENCE",
                    List.of(
                            new GrowthPoint(LAST_OBSERVED_YEAR, industryIndexes.get(LAST_OBSERVED_YEAR)),
                            new GrowthPoint(REFERENCE_YEAR, industryReferenceIndex2025))));
        }
        return lines;
    }

    private Map<Integer, Double> companyIndexByYear(Map<Integer, Integer> salesByYear) {
        Integer baseSales = salesByYear.get(BASE_YEAR);
        Map<Integer, Double> indexes = new HashMap<>();
        for (int year = BASE_YEAR; year <= LAST_OBSERVED_YEAR; year++) {
            Integer sales = salesByYear.get(year);
            indexes.put(year, baseSales == null || baseSales == 0 || sales == null
                    ? null
                    : round(sales * 100.0 / baseSales));
        }
        return indexes;
    }

    private Map<Integer, Double> companyGrowthRateByYear(Map<Integer, Integer> salesByYear) {
        Map<Integer, Double> rates = new HashMap<>();
        for (int year = BASE_YEAR + 1; year <= LAST_OBSERVED_YEAR; year++) {
            rates.put(year, growthPercent(salesByYear.get(year - 1), salesByYear.get(year)));
        }
        return rates;
    }

    private Map<Integer, Double> industryGrowthRateByYear(String bokIndustryCode) {
        Map<Integer, Double> rates = new HashMap<>();
        if (bokIndustryCode == null || bokIndustryCode.isBlank()) {
            return rates;
        }
        benchmarkMetricRepository.findByBokIndustryCodeAndMetricOrderByYearAsc(bokIndustryCode, REVENUE_GROWTH_RATE).stream()
                .filter(metric -> metric.getYear() != null)
                .forEach(metric -> rates.put(
                        metric.getYear(), metric.getValue() == null ? null : metric.getValue().doubleValue()));
        return rates;
    }

    private Map<Integer, Double> industryIndexByYear(Map<Integer, Double> industryGrowthRates) {
        Map<Integer, Double> indexes = new HashMap<>();
        if (industryGrowthRates.isEmpty()) {
            return indexes;
        }
        indexes.put(BASE_YEAR, 100.0);
        for (int year = BASE_YEAR + 1; year <= LAST_OBSERVED_YEAR; year++) {
            Double previousIndex = indexes.get(year - 1);
            Double growthRate = industryGrowthRates.get(year);
            indexes.put(year, previousIndex == null || growthRate == null
                    ? null
                    : round(previousIndex * (1 + growthRate / 100.0)));
        }
        return indexes;
    }

    private List<GrowthPoint> actualPoints(Map<Integer, Double> indexes) {
        return List.of(BASE_YEAR, 2022, 2023, LAST_OBSERVED_YEAR).stream()
                .map(year -> new GrowthPoint(year, indexes.get(year)))
                .toList();
    }

    private Double industryReferenceGrowthRate2025(Map<Integer, Double> industryGrowthRates) {
        List<Double> rates = List.of(2022, 2023, LAST_OBSERVED_YEAR).stream()
                .map(industryGrowthRates::get)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        return rates.size() < 3 ? null : median(rates);
    }

    private Double weightedCorrection(Map<Integer, Double> companyGrowthRates, Map<Integer, Double> industryGrowthRates) {
        Double gap2023 = industryGap(2023, companyGrowthRates, industryGrowthRates);
        Double gap2024 = industryGap(LAST_OBSERVED_YEAR, companyGrowthRates, industryGrowthRates);
        if (gap2023 == null || gap2024 == null) {
            return 0.0;
        }
        Double industryGapAverage = (gap2023 + gap2024) / 2.0;
        Double spread = Math.abs(gap2023 - gap2024);
        Double z = 1 / (1 + Math.pow(spread / STABILITY_SCALE_C, 2));
        return z * industryGapAverage;
    }

    private Double industryGap(Integer year, Map<Integer, Double> companyGrowthRates, Map<Integer, Double> industryGrowthRates) {
        Double companyGrowthRate = companyGrowthRates.get(year);
        Double industryGrowthRate = industryGrowthRates.get(year);
        return companyGrowthRate == null || industryGrowthRate == null ? null : companyGrowthRate - industryGrowthRate;
    }

    private Double referenceIndex(Double baseIndex, Double growthRate) {
        return baseIndex == null || growthRate == null ? null : round(baseIndex * (1 + growthRate / 100.0));
    }

    private SupportMarker supportMarker(
            BtpSupportHistory history,
            Map<Integer, CompanyFinancialStatistics> financialsByYear,
            Map<Integer, Integer> employeesByYear) {
        Integer markerYear = markerYear(history);
        CompanyFinancialStatistics markerFinancial = markerYear == null ? null : financialsByYear.get(markerYear);
        return new SupportMarker(
                history.getSupportHistoryId(),
                history.getBudgetProgramName(),
                history.getSupportType(),
                formatDate(history.getStartDate()),
                formatDate(history.getEndDate()),
                history.getSupportAmount(),
                MoneyUnits.KRW_THOUSAND,
                markerYear,
                markerMonth(history),
                new MetricsAtMarker(
                        markerFinancial == null ? null : markerFinancial.getResearchAndDevelopmentExpense(),
                        markerFinancial == null ? null : round(markerFinancial.getOperatingMargin()),
                        markerYear == null ? null : employeesByYear.get(markerYear)));
    }

    private Integer markerYear(BtpSupportHistory history) {
        if (history.getStartDate() != null) {
            return history.getStartDate().getYear();
        }
        return history.getSupportYear();
    }

    private Integer markerMonth(BtpSupportHistory history) {
        return history.getStartDate() == null ? null : history.getStartDate().getMonthValue();
    }

    private ObservedFlow observedFlow(
            Map<Integer, CompanyFinancialStatistics> financialsByYear,
            Map<Integer, Integer> employeesByYear) {
        List<ObservedFlowPeriod> periods = List.of(
                new ObservedFlowPeriod(2021, 2022),
                new ObservedFlowPeriod(2022, 2023),
                new ObservedFlowPeriod(2023, LAST_OBSERVED_YEAR));
        return new ObservedFlow(
                periods,
                List.of(
                        observedFlowRow(
                                "RND_EXPENSE",
                                periods,
                                year -> {
                                    CompanyFinancialStatistics stat = financialsByYear.get(year);
                                    return stat == null ? null : stat.getResearchAndDevelopmentExpense();
                                }),
                        observedFlowRow(
                                "OPERATING_PROFIT_MARGIN",
                                periods,
                                year -> {
                                    CompanyFinancialStatistics stat = financialsByYear.get(year);
                                    return stat == null ? null : stat.getOperatingMargin();
                                }),
                        observedFlowRow("EMPLOYEE_COUNT", periods, employeesByYear::get)));
    }

    private ObservedFlowRow observedFlowRow(
            String code,
            List<ObservedFlowPeriod> periods,
            java.util.function.Function<Integer, Number> valueByYear) {
        return new ObservedFlowRow(
                code,
                periods.stream()
                        .map(period -> new ObservedFlowDirection(
                                period.fromYear(),
                                period.toYear(),
                                direction(valueByYear.apply(period.fromYear()), valueByYear.apply(period.toYear()))))
                        .toList());
    }

    private String direction(Number fromValue, Number toValue) {
        if (fromValue == null || toValue == null) {
            return "NO_DATA";
        }
        int comparison = Double.compare(toValue.doubleValue(), fromValue.doubleValue());
        if (comparison > 0) {
            return "UP";
        }
        if (comparison < 0) {
            return "DOWN";
        }
        return "FLAT";
    }
    private ComputedMetricItem metric(String code, String label, Double value, String unit) {
        return new ComputedMetricItem(code, label, round(value), unit);
    }

    private Integer maxReferenceYear(
            List<CompanyNtisLeadProject> leadProjects, List<CompanyNtisCollaborativeProject> collaborativeProjects) {
        return java.util.stream.Stream.concat(
                        leadProjects.stream().map(CompanyNtisLeadProject::getReferenceYear),
                        collaborativeProjects.stream().map(CompanyNtisCollaborativeProject::getReferenceYear))
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private Double median(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) {
            return values.get(middle);
        }
        return (values.get(middle - 1) + values.get(middle)) / 2;
    }

    private Double growthPercent(Integer previous, Integer current) {
        if (previous == null || current == null || previous == 0) {
            return null;
        }
        return round((current - previous) * 100.0 / previous);
    }

    private Double round(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : BASIC_DATE.format(date);
    }

    private String registeredDate(Boolean exists, LocalDate registeredDate) {
        return Boolean.TRUE.equals(exists) ? formatDate(registeredDate) : null;
    }

    private Integer companyAge(LocalDate establishedDate, LocalDate referenceDate) {
        if (establishedDate == null) {
            return null;
        }
        LocalDate baseDate = referenceDate == null ? LocalDate.now() : referenceDate;
        int age = baseDate.getYear() - establishedDate.getYear();
        return baseDate.getDayOfYear() < establishedDate.getDayOfYear() ? age - 1 : age;
    }

}
