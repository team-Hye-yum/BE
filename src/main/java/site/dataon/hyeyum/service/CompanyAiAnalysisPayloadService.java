package site.dataon.hyeyum.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import site.dataon.hyeyum.common.PatentRegistrationStatuses;
import site.dataon.hyeyum.domain.BtpSupportHistory;
import site.dataon.hyeyum.domain.Company;
import site.dataon.hyeyum.domain.CompanyBusinessPurpose;
import site.dataon.hyeyum.domain.CompanyEmploymentStatistics;
import site.dataon.hyeyum.domain.CompanyFinancialStatistics;
import site.dataon.hyeyum.domain.CompanyIndustryBenchmarkMapping;
import site.dataon.hyeyum.domain.CompanyNtisCollaborativeProject;
import site.dataon.hyeyum.domain.CompanyNtisLeadProject;
import site.dataon.hyeyum.domain.CompanyPatent;
import site.dataon.hyeyum.domain.IndustryBenchmarkMetric;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.Capabilities;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.Employment;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.Financials;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.IndustryComparison;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.IndustryGrowthPoint;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.Options;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.PatentSummary;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.Profile;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.ResearchOrganizations;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.SupportHistory;
import site.dataon.hyeyum.repository.BtpSupportHistoryRepository;
import site.dataon.hyeyum.repository.CompanyBusinessPurposeRepository;
import site.dataon.hyeyum.repository.CompanyEmploymentStatisticsRepository;
import site.dataon.hyeyum.repository.CompanyFinancialStatisticsRepository;
import site.dataon.hyeyum.repository.CompanyIndustryBenchmarkMappingRepository;
import site.dataon.hyeyum.repository.CompanyNtisCollaborativeProjectRepository;
import site.dataon.hyeyum.repository.CompanyNtisLeadProjectRepository;
import site.dataon.hyeyum.repository.CompanyPatentRepository;
import site.dataon.hyeyum.repository.CompanyRepository;
import site.dataon.hyeyum.repository.IndustryBenchmarkMetricRepository;

@Service
public class CompanyAiAnalysisPayloadService {

    private static final int INDUSTRY_COMPARISON_BASE_YEAR = 2021;
    private static final int INDUSTRY_COMPARISON_LATEST_YEAR = 2024;
    private static final String REVENUE_GROWTH_RATE = "REVENUE_GROWTH_RATE";
    private static final List<String> SELECTED_RESULTS = List.of("지원대상", "선정");
    private static final List<String> MARKET_KEYWORDS =
            List.of("전시", "수출", "마케팅", "판로", "해외", "브랜드", "홍보", "시장", "바이어");
    private static final List<String> TECH_RND_KEYWORDS =
            List.of("기술", "R&D", "연구", "개발", "시제품", "장비", "시험", "인증", "특허");
    private static final List<String> JOB_CREATION_KEYWORDS =
            List.of("일자리", "고용", "인건비", "채용", "인력");

    private final CompanyRepository companyRepository;
    private final BtpSupportHistoryRepository supportHistoryRepository;
    private final CompanyEmploymentStatisticsRepository employmentStatisticsRepository;
    private final CompanyFinancialStatisticsRepository financialStatisticsRepository;
    private final CompanyBusinessPurposeRepository businessPurposeRepository;
    private final CompanyPatentRepository patentRepository;
    private final CompanyNtisLeadProjectRepository ntisLeadProjectRepository;
    private final CompanyNtisCollaborativeProjectRepository ntisCollaborativeProjectRepository;
    private final CompanyIndustryBenchmarkMappingRepository benchmarkMappingRepository;
    private final IndustryBenchmarkMetricRepository benchmarkMetricRepository;

    public CompanyAiAnalysisPayloadService(
            CompanyRepository companyRepository,
            BtpSupportHistoryRepository supportHistoryRepository,
            CompanyEmploymentStatisticsRepository employmentStatisticsRepository,
            CompanyFinancialStatisticsRepository financialStatisticsRepository,
            CompanyBusinessPurposeRepository businessPurposeRepository,
            CompanyPatentRepository patentRepository,
            CompanyNtisLeadProjectRepository ntisLeadProjectRepository,
            CompanyNtisCollaborativeProjectRepository ntisCollaborativeProjectRepository,
            CompanyIndustryBenchmarkMappingRepository benchmarkMappingRepository,
            IndustryBenchmarkMetricRepository benchmarkMetricRepository) {
        this.companyRepository = companyRepository;
        this.supportHistoryRepository = supportHistoryRepository;
        this.employmentStatisticsRepository = employmentStatisticsRepository;
        this.financialStatisticsRepository = financialStatisticsRepository;
        this.businessPurposeRepository = businessPurposeRepository;
        this.patentRepository = patentRepository;
        this.ntisLeadProjectRepository = ntisLeadProjectRepository;
        this.ntisCollaborativeProjectRepository = ntisCollaborativeProjectRepository;
        this.benchmarkMappingRepository = benchmarkMappingRepository;
        this.benchmarkMetricRepository = benchmarkMetricRepository;
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<CompanyAiAnalysisPayloadResponse> payload(Integer companyId) {
        Company company = companyRepository
                .findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found."));
        List<BtpSupportHistory> supportHistories =
                supportHistoryRepository.findByCompanyIdOrderBySupportYearAscSupportHistoryIdAsc(companyId);
        List<BtpSupportHistory> selectedHistories = supportHistories.stream()
                .filter(this::isSelected)
                .toList();
        List<CompanyEmploymentStatistics> employmentStatistics =
                employmentStatisticsRepository.findByCompanyIdOrderByYearAsc(companyId);
        List<CompanyFinancialStatistics> financialStatistics =
                financialStatisticsRepository.findByCompanyIdOrderByYearAsc(companyId);
        List<CompanyBusinessPurpose> businessPurposes =
                businessPurposeRepository.findByCompanyIdOrderByDisplayOrderAscBusinessPurposeIdAsc(companyId);
        List<CompanyNtisLeadProject> leadProjects =
                ntisLeadProjectRepository.findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisLeadProjectIdAsc(companyId);
        List<CompanyNtisCollaborativeProject> collaborativeProjects =
                ntisCollaborativeProjectRepository
                        .findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisCollaborativeProjectIdAsc(companyId);

        CompanyAiAnalysisPayloadResponse response = new CompanyAiAnalysisPayloadResponse(
                company.getCompanyId(),
                profile(company),
                capabilities(company, businessPurposes, leadProjects, collaborativeProjects),
                financials(company, financialStatistics),
                industryComparison(company.getCompanyId(), financialStatistics),
                employment(employmentStatistics),
                supportHistory(supportHistories, selectedHistories),
                new Options(3));
        return new ApiDataResponse<>(response);
    }

    private Profile profile(Company company) {
        return new Profile(
                company.getIndustryName(),
                company.getIndustryBrief(),
                company.getKsicCode(),
                company.getRegionName(),
                company.getEstablishedDate(),
                company.getCompanySize(),
                company.getMainProduct());
    }

    private Capabilities capabilities(
            Company company,
            List<CompanyBusinessPurpose> businessPurposes,
            List<CompanyNtisLeadProject> leadProjects,
            List<CompanyNtisCollaborativeProject> collaborativeProjects) {
        List<String> purposes = businessPurposes.stream()
                .map(CompanyBusinessPurpose::getBusinessPurpose)
                .filter(this::hasText)
                .limit(5)
                .toList();
        List<String> ntisProjectNames = leadProjects.stream()
                .map(CompanyNtisLeadProject::getProjectName)
                .filter(this::hasText)
                .limit(5)
                .toList();
        int ntisProjectCount = leadProjects.size() + collaborativeProjects.size();
        return new Capabilities(
                purposes,
                ntisProjectNames,
                ntisProjectCount,
                patentSummary(company.getCompanyId()),
                new ResearchOrganizations(
                        company.getHasResearchLab(),
                        company.getHasRndDepartment(),
                        company.getResearcherCount()));
    }

    private Financials financials(Company company, List<CompanyFinancialStatistics> financialStatistics) {
        CompanyFinancialStatistics latest = financialStatistics.stream()
                .filter(stat -> stat.getYear() != null)
                .max(Comparator.comparing(CompanyFinancialStatistics::getYear))
                .orElse(null);
        return new Financials(
                latest == null ? null : latest.getYear(),
                latest == null ? null : latest.getSalesAmount(),
                round(company.getSalesGrowthRate()),
                round(company.getSupportedSalesGrowthRate()),
                round(company.getDebtRatio()),
                round(company.getGovernmentRndDependency()),
                latest == null ? null : latest.getResearchAndDevelopmentExpense());
    }

    private IndustryComparison industryComparison(
            Integer companyId,
            List<CompanyFinancialStatistics> financialStatistics) {
        Map<Integer, Long> salesByYear = new HashMap<>();
        financialStatistics.stream()
                .filter(stat -> stat.getYear() != null)
                .forEach(stat -> salesByYear.put(stat.getYear(), stat.getSalesAmount()));

        Map<Integer, Double> companyIndexes = companyIndexByYear(salesByYear);
        Map<Integer, Double> companyGrowthRates = companyGrowthRateByYear(salesByYear);
        String bokIndustryCode = benchmarkMappingRepository
                .findById(companyId)
                .map(CompanyIndustryBenchmarkMapping::getBokIndustryCode)
                .orElse(null);
        Map<Integer, Double> industryGrowthRates = industryGrowthRateByYear(bokIndustryCode);
        Map<Integer, Double> industryIndexes = industryIndexByYear(industryGrowthRates);

        List<IndustryGrowthPoint> points = new ArrayList<>();
        for (int year = INDUSTRY_COMPARISON_BASE_YEAR; year <= INDUSTRY_COMPARISON_LATEST_YEAR; year++) {
            points.add(new IndustryGrowthPoint(
                    year,
                    companyIndexes.get(year),
                    industryIndexes.get(year),
                    companyGrowthRates.get(year),
                    industryGrowthRates.get(year)));
        }

        Double companyChangeRate = indexChangeRate(companyIndexes.get(INDUSTRY_COMPARISON_LATEST_YEAR));
        Double industryChangeRate = indexChangeRate(industryIndexes.get(INDUSTRY_COMPARISON_LATEST_YEAR));
        Double gapRate = companyChangeRate == null || industryChangeRate == null
                ? null
                : round(companyChangeRate - industryChangeRate);
        return new IndustryComparison(
                INDUSTRY_COMPARISON_BASE_YEAR,
                INDUSTRY_COMPARISON_LATEST_YEAR,
                companyChangeRate,
                industryChangeRate,
                gapRate,
                points,
                industryComparisonSummary(companyChangeRate, industryChangeRate, gapRate));
    }

    private Employment employment(List<CompanyEmploymentStatistics> employmentStatistics) {
        CompanyEmploymentStatistics latest = employmentStatistics.stream()
                .filter(stat -> stat.getYear() != null)
                .max(Comparator.comparing(CompanyEmploymentStatistics::getYear))
                .orElse(null);
        if (latest == null) {
            return new Employment(null, null, null, null, null, null, null);
        }
        Integer previousEmployeeCount = employmentStatistics.stream()
                .filter(stat -> Objects.equals(stat.getYear(), latest.getYear() - 1))
                .map(CompanyEmploymentStatistics::getEmployeeCount)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return new Employment(
                latest.getYear(),
                previousEmployeeCount,
                latest.getEmployeeCount(),
                latest.getPensionSubscriberCount(),
                latest.getPensionNewHireCount(),
                latest.getPensionRetireeCount(),
                round(calculateTurnoverRate(previousEmployeeCount, latest)));
    }

    private SupportHistory supportHistory(List<BtpSupportHistory> supportHistories, List<BtpSupportHistory> selectedHistories) {
        List<String> recentSupportTexts = supportHistories.stream()
                .sorted(Comparator.comparing(BtpSupportHistory::getSupportYear, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BtpSupportHistory::getSelectedDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BtpSupportHistory::getSupportHistoryId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::supportText)
                .filter(this::hasText)
                .limit(10)
                .toList();
        return new SupportHistory(
                supportHistories.size(),
                countByKeywords(selectedHistories, MARKET_KEYWORDS),
                countByKeywords(selectedHistories, TECH_RND_KEYWORDS),
                selectedHistories.stream().anyMatch(history -> containsAny(supportText(history), JOB_CREATION_KEYWORDS)),
                recentSupportTexts);
    }

    private PatentSummary patentSummary(Integer companyId) {
        long activeCount = patentRepository.countByCompanyIdAndRegistrationStatusAndIsActiveTrue(
                companyId, PatentRegistrationStatuses.REGISTERED);
        Integer latestRegistrationYear = patentRepository.findByCompanyIdOrderByRegistrationDateDescPatentIdAsc(companyId)
                .stream()
                .map(CompanyPatent::getRegistrationDate)
                .filter(Objects::nonNull)
                .map(LocalDate::getYear)
                .findFirst()
                .orElse(null);
        return new PatentSummary(activeCount, latestRegistrationYear);
    }

    private Double calculateTurnoverRate(Integer previousEmployeeCount, CompanyEmploymentStatistics latest) {
        Double averageWorkforce = averageWorkforce(previousEmployeeCount, latest);
        if (averageWorkforce == null || averageWorkforce <= 0 || latest.getPensionRetireeCount() == null) {
            return null;
        }
        return latest.getPensionRetireeCount() * 100.0 / averageWorkforce;
    }

    private Map<Integer, Double> companyIndexByYear(Map<Integer, Long> salesByYear) {
        Long baseSales = salesByYear.get(INDUSTRY_COMPARISON_BASE_YEAR);
        Map<Integer, Double> indexes = new HashMap<>();
        for (int year = INDUSTRY_COMPARISON_BASE_YEAR; year <= INDUSTRY_COMPARISON_LATEST_YEAR; year++) {
            Long sales = salesByYear.get(year);
            indexes.put(year, baseSales == null || baseSales == 0 || sales == null
                    ? null
                    : round(sales * 100.0 / baseSales));
        }
        return indexes;
    }

    private Map<Integer, Double> companyGrowthRateByYear(Map<Integer, Long> salesByYear) {
        Map<Integer, Double> rates = new HashMap<>();
        for (int year = INDUSTRY_COMPARISON_BASE_YEAR + 1; year <= INDUSTRY_COMPARISON_LATEST_YEAR; year++) {
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
                .forEach(metric -> rates.put(metric.getYear(), metricValue(metric)));
        return rates;
    }

    private Map<Integer, Double> industryIndexByYear(Map<Integer, Double> industryGrowthRates) {
        Map<Integer, Double> indexes = new HashMap<>();
        if (industryGrowthRates.isEmpty()) {
            return indexes;
        }
        indexes.put(INDUSTRY_COMPARISON_BASE_YEAR, 100.0);
        for (int year = INDUSTRY_COMPARISON_BASE_YEAR + 1; year <= INDUSTRY_COMPARISON_LATEST_YEAR; year++) {
            Double previousIndex = indexes.get(year - 1);
            Double growthRate = industryGrowthRates.get(year);
            indexes.put(year, previousIndex == null || growthRate == null
                    ? null
                    : round(previousIndex * (1 + growthRate / 100.0)));
        }
        return indexes;
    }

    private Double indexChangeRate(Double index) {
        return index == null ? null : round(index - 100.0);
    }

    private Double growthPercent(Number previous, Number current) {
        if (previous == null || current == null || previous.doubleValue() == 0) {
            return null;
        }
        return round((current.doubleValue() - previous.doubleValue()) * 100.0 / previous.doubleValue());
    }

    private Double metricValue(IndustryBenchmarkMetric metric) {
        return metric.getValue() == null ? null : round(metric.getValue().doubleValue());
    }

    private String industryComparisonSummary(Double companyChangeRate, Double industryChangeRate, Double gapRate) {
        if (companyChangeRate == null || industryChangeRate == null || gapRate == null) {
            return "산업 대비 매출 흐름을 비교할 데이터가 제한적입니다.";
        }
        if (industryChangeRate <= -15 && gapRate >= 10) {
            return "산업 하락폭보다 기업 매출 하락폭이 작거나 더 나은 흐름을 보여 방어력을 중심으로 볼 수 있습니다.";
        }
        if (industryChangeRate >= 15 && gapRate <= -10) {
            return "산업은 확장됐지만 기업 매출 흐름은 상대적으로 약해 시장 호황을 따라갔는지 확인할 수 있습니다.";
        }
        if (gapRate >= 10) {
            return "기업 매출 흐름이 산업 평균보다 뚜렷하게 앞서므로 기업 고유 성장 요인을 함께 볼 수 있습니다.";
        }
        if (gapRate <= -10) {
            return "기업 매출 흐름이 산업 평균보다 낮아 업종 환경과 기업 내부 요인을 나누어 볼 수 있습니다.";
        }
        return "기업 매출 흐름과 산업 평균 흐름이 큰 차이 없이 움직여 다른 역량 지표와 함께 해석할 수 있습니다.";
    }

    private Double averageWorkforce(Integer previousEmployeeCount, CompanyEmploymentStatistics latest) {
        if (previousEmployeeCount != null && latest.getEmployeeCount() != null) {
            return (previousEmployeeCount + latest.getEmployeeCount()) / 2.0;
        }
        if (latest.getPensionSubscriberCount() != null) {
            return latest.getPensionSubscriberCount().doubleValue();
        }
        return latest.getEmployeeCount() == null ? null : latest.getEmployeeCount().doubleValue();
    }

    private int countByKeywords(List<BtpSupportHistory> histories, List<String> keywords) {
        return (int) histories.stream()
                .filter(history -> containsAny(supportText(history), keywords))
                .count();
    }

    private String supportText(BtpSupportHistory history) {
        List<String> parts = new ArrayList<>();
        if (history.getSupportYear() != null) {
            parts.add(String.valueOf(history.getSupportYear()));
        }
        parts.addAll(List.of(
                nullToEmpty(history.getBudgetProgramName()),
                nullToEmpty(history.getSupportType()),
                nullToEmpty(history.getSupportCategory()),
                nullToEmpty(history.getSupportDetail()),
                nullToEmpty(history.getSupportItem()),
                nullToEmpty(history.getSelectionResult())));
        return joinText(parts.toArray(String[]::new));
    }

    private boolean isSelected(BtpSupportHistory history) {
        return SELECTED_RESULTS.contains(history.getSelectionResult());
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (!hasText(text)) {
            return false;
        }
        String lower = text.toLowerCase();
        return keywords.stream().anyMatch(keyword -> lower.contains(keyword.toLowerCase()));
    }

    private String joinText(String... values) {
        return Stream.of(values)
                .filter(this::hasText)
                .reduce("", (left, right) -> hasText(left) ? left + " " + right.trim() : right.trim());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Double round(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
