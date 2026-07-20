package site.dataon.hyeyum.service;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
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
import site.dataon.hyeyum.dto.CompanyDashboardResponses.MoneyValue;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NewsItem;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NewsResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisCollaborativeProjectItem;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisCollaborativeProjectListResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisLeadProjectItem;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisLeadProjectListResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisSummary;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.PatentItem;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.PatentListResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ProductiveActivitiesSummaryResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ResearchOrganization;
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
    private static final String KRW = "KRW";
    private static final String KRW_THOUSAND = "KRW_THOUSAND";
    private static final String REVENUE_INDEX = "REVENUE_INDEX";
    private static final String REVENUE_GROWTH_RATE = "REVENUE_GROWTH_RATE";
    private static final Integer BASE_YEAR = 2021;
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
    private final HttpClient httpClient;

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
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
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
        return new ApiDataResponse<>(new FinancialPositionResponse(companyId, KRW_THOUSAND, series));
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
        return new ApiDataResponse<>(new IncomeStatementsResponse(companyId, KRW_THOUSAND, series));
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
        long activePatentCount = patentRepository.countByCompanyIdAndRegistrationStatusAndIsActiveTrue(companyId, "등록");
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
                new MoneyValue(recentFiveYearFund, KRW),
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
                        new MoneyValue(project.getGovernmentResearchFund(), KRW),
                        new MoneyValue(project.getPrivateResearchFund(), KRW),
                        new MoneyValue(project.getTotalResearchFund(), KRW)))
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
                        new MoneyValue(project.getCommissionedResearchFund(), KRW),
                        new MoneyValue(project.getCollaborativeResearchExpense(), KRW),
                        new MoneyValue(project.getCollaborativeResearchIncome(), KRW),
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
                        metric("DEBT_RATIO", "부채 비율", company.getDebtRatio(), "PERCENT"),
                        metric("COST_OF_SALES_RATIO", "매출 원가율", company.getCostOfSalesRatio(), "PERCENT"),
                        metric("SALES_GROWTH", "매출 성장성", company.getSalesGrowthRate(), "PERCENT"),
                        metric("EMPLOYMENT_GROWTH", "고용 성장성", company.getEmploymentGrowthRate(), "PERCENT"),
                        metric("GOVERNMENT_RD_DEPENDENCY", "정부 R&D 의존도", company.getGovernmentRndDependency(), "PERCENT"),
                        metric("SUPPORTED_COMPANY_SALES_CHANGE_RATE", "지원 기업 매출 변화율", company.getSupportedSalesGrowthRate(), "PERCENT"),
                        metric("EMPLOYMENT_DIVERGENCE_INDEX", "고용 괴리 지수", company.getEmploymentPeakIndex(), "PERCENT_POINT"),
                        metric("EMPLOYMENT_TURNOVER_RATE", "고용 회전율", company.getEmployeeTurnoverRate(), "PERCENT"))));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<GrowthScenarioResponse> growthScenario(Integer companyId) {
        findCompany(companyId);
        List<CompanyFinancialStatistics> financials = financialStatisticsRepository.findByCompanyIdOrderByYearAsc(companyId);
        Map<Integer, Integer> salesByYear = new HashMap<>();
        financials.stream()
                .filter(stat -> stat.getYear() != null)
                .forEach(stat -> salesByYear.put(stat.getYear(), stat.getSalesAmount()));
        String bokIndustryCode = benchmarkMappingRepository
                .findById(companyId)
                .map(CompanyIndustryBenchmarkMapping::getBokIndustryCode)
                .orElse(null);
        List<GrowthPoint> companyPoints = companyGrowthPoints(salesByYear, bokIndustryCode);
        List<GrowthPoint> industryPoints = industryGrowthPoints(bokIndustryCode);
        List<SupportMarker> markers = supportHistoryRepository.findByCompanyIdOrderBySupportYearAscSupportHistoryIdAsc(companyId).stream()
                .map(history -> new SupportMarker(
                        history.getSupportHistoryId(),
                        history.getBudgetProgramName(),
                        history.getSupportItem(),
                        formatDate(history.getStartDate()),
                        formatDate(history.getEndDate()),
                        history.getSupportYear()))
                .toList();
        return new ApiDataResponse<>(new GrowthScenarioResponse(
                companyId,
                DISCLAIMER,
                List.of(
                        new GrowthChartLine("COMPANY", "기업 성장 경로", companyPoints),
                        new GrowthChartLine("INDUSTRY", "동일 업종 평균 성장 경로", industryPoints)),
                markers));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<AiSummaryResponse> aiSummary(Integer companyId) {
        return new ApiDataResponse<>(new AiSummaryResponse(findCompany(companyId).getAiSummary()));
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<NewsResponse> news(Integer companyId, Integer limit) {
        Company company = findCompany(companyId);
        int resolvedLimit = Math.max(1, Math.min(Optional.ofNullable(limit).orElse(3), 10));
        if (company.getCompanyName() == null || company.getCompanyName().isBlank()) {
            return new ApiDataResponse<>(new NewsResponse(companyId, company.getCompanyName(), "MISSING_COMPANY_NAME", List.of()));
        }
        try {
            return new ApiDataResponse<>(new NewsResponse(
                    companyId, company.getCompanyName(), "OK", fetchGoogleNews(company.getCompanyName(), resolvedLimit)));
        } catch (Exception exception) {
            return new ApiDataResponse<>(new NewsResponse(companyId, company.getCompanyName(), "RSS_FETCH_FAILED", List.of()));
        }
    }

    private Company findCompany(Integer companyId) {
        return companyRepository
                .findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "기업 정보를 찾을 수 없습니다."));
    }

    private List<GrowthPoint> companyGrowthPoints(Map<Integer, Integer> salesByYear, String bokIndustryCode) {
        Integer baseSales = salesByYear.get(BASE_YEAR);
        List<GrowthPoint> points = salesByYear.entrySet().stream()
                .filter(entry -> entry.getKey() >= BASE_YEAR)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new GrowthPoint(
                        entry.getKey(),
                        entry.getValue(),
                        baseSales == null || baseSales == 0 || entry.getValue() == null
                                ? null
                                : round(entry.getValue() * 100.0 / baseSales),
                        "ACTUAL"))
                .collect(Collectors.toCollection(ArrayList::new));

        Integer latestYear = salesByYear.keySet().stream().filter(year -> year >= BASE_YEAR).max(Integer::compareTo).orElse(null);
        if (latestYear != null) {
            Integer nextYear = latestYear + 1;
            Double predictedGrowth = predictedGrowthRate(salesByYear, bokIndustryCode, nextYear);
            Double latestIndex = points.stream()
                    .filter(point -> Objects.equals(point.year(), latestYear))
                    .map(GrowthPoint::index)
                    .findFirst()
                    .orElse(null);
            if (predictedGrowth != null && latestIndex != null) {
                points.add(new GrowthPoint(nextYear, null, round(latestIndex * (1 + predictedGrowth / 100.0)), "REFERENCE_PATH"));
            }
        }
        return points;
    }

    private List<GrowthPoint> industryGrowthPoints(String bokIndustryCode) {
        if (bokIndustryCode == null || bokIndustryCode.isBlank()) {
            return List.of();
        }
        return benchmarkIndexRepository
                .findByBokIndustryCodeAndMetricAndBaseYearOrderByYearAsc(bokIndustryCode, REVENUE_INDEX, BASE_YEAR)
                .stream()
                .map(index -> new GrowthPoint(
                        index.getYear(),
                        null,
                        index.getIndexValue() == null ? null : round(index.getIndexValue().doubleValue()),
                        index.getYear() != null && index.getYear() > 2024 ? "REFERENCE_PATH" : "ACTUAL"))
                .toList();
    }

    private Double predictedGrowthRate(Map<Integer, Integer> salesByYear, String bokIndustryCode, Integer targetYear) {
        if (bokIndustryCode == null || targetYear == null) {
            return null;
        }
        Map<Integer, Double> industryGrowthRates = industryGrowthRateByYear(bokIndustryCode);
        Double targetIndustryGrowthRate = industryGrowthRates.get(targetYear);
        if (targetIndustryGrowthRate == null) {
            return null;
        }
        List<Double> excessRates = List.of(targetYear - 2, targetYear - 1).stream()
                .map(year -> {
                    Double companyGrowthRate = growthPercent(salesByYear.get(year - 1), salesByYear.get(year));
                    Double industryGrowthRate = industryGrowthRates.get(year);
                    return companyGrowthRate == null || industryGrowthRate == null ? null : companyGrowthRate - industryGrowthRate;
                })
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        Double structuralExcessRate = median(excessRates);
        return structuralExcessRate == null ? targetIndustryGrowthRate : targetIndustryGrowthRate + structuralExcessRate;
    }

    private Map<Integer, Double> industryGrowthRateByYear(String bokIndustryCode) {
        Map<Integer, Double> rates = new HashMap<>();
        benchmarkMetricRepository.findByBokIndustryCodeAndMetricOrderByYearAsc(bokIndustryCode, REVENUE_GROWTH_RATE).stream()
                .filter(metric -> metric.getYear() != null)
                .forEach(metric -> rates.put(
                        metric.getYear(), metric.getValue() == null ? null : metric.getValue().doubleValue()));
        return rates;
    }

    private List<NewsItem> fetchGoogleNews(String companyName, int limit) throws Exception {
        String query = URLEncoder.encode(companyName, StandardCharsets.UTF_8);
        URI uri = URI.create("https://news.google.com/rss/search?q=" + query + "&hl=ko&gl=KR&ceid=KR:ko");
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            return List.of();
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        NodeList itemNodes = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(response.body()))
                .getElementsByTagName("item");
        List<NewsItem> items = new ArrayList<>();
        for (int index = 0; index < itemNodes.getLength() && items.size() < limit; index++) {
            Element item = (Element) itemNodes.item(index);
            items.add(new NewsItem(
                    text(item, "title"),
                    text(item, "source"),
                    parseRssDate(text(item, "pubDate")),
                    text(item, "link")));
        }
        return items;
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

    private Integer companyAge(LocalDate establishedDate, LocalDate referenceDate) {
        if (establishedDate == null) {
            return null;
        }
        LocalDate baseDate = referenceDate == null ? LocalDate.now() : referenceDate;
        int age = baseDate.getYear() - establishedDate.getYear();
        return baseDate.getDayOfYear() < establishedDate.getDayOfYear() ? age - 1 : age;
    }

    private String text(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private OffsetDateTime parseRssDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("EEE, dd MMM yyyy HH:mm:ss zzz")
                    .toFormatter(java.util.Locale.ENGLISH);
            return OffsetDateTime.from(formatter.parse(value));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
