package site.dataon.hyeyum.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.BusanRewindResponses.BusanRelevance;
import site.dataon.hyeyum.dto.BusanRewindResponses.ChangedField;
import site.dataon.hyeyum.dto.BusanRewindResponses.ChangeComparison;
import site.dataon.hyeyum.dto.BusanRewindResponses.CountRatio;
import site.dataon.hyeyum.dto.BusanRewindResponses.CurrentStatus;
import site.dataon.hyeyum.dto.BusanRewindResponses.CurrentSupportProgram;
import site.dataon.hyeyum.dto.BusanRewindResponses.CurrentSupportPrograms;
import site.dataon.hyeyum.dto.BusanRewindResponses.DistrictEmployeeGrowth;
import site.dataon.hyeyum.dto.BusanRewindResponses.EmployeeSizeRatio;
import site.dataon.hyeyum.dto.BusanRewindResponses.GrowthPoint;
import site.dataon.hyeyum.dto.BusanRewindResponses.IndexPoint;
import site.dataon.hyeyum.dto.BusanRewindResponses.IndustryChange;
import site.dataon.hyeyum.dto.BusanRewindResponses.PastSupportProgram;
import site.dataon.hyeyum.dto.BusanRewindResponses.PastSupportReview;
import site.dataon.hyeyum.dto.BusanRewindResponses.Period;
import site.dataon.hyeyum.dto.BusanRewindResponses.PeriodHighlight;
import site.dataon.hyeyum.dto.BusanRewindResponses.SimilarFlow;
import site.dataon.hyeyum.dto.BusanRewindResponses.SupportComparison;
import site.dataon.hyeyum.dto.BusanRewindResponses.SupportedCompanyChange;
import site.dataon.hyeyum.dto.BusanRewindResponses.TrendBriefing;
import site.dataon.hyeyum.dto.BusanRewindResponses.TrendDomestic;
import site.dataon.hyeyum.dto.BusanRewindResponses.TrendIssues;

@Service
public class BusanRewindService {

    private static final String TOTAL_DIMENSION = "계";
    private static final Map<String, String> BUSAN_SGG_CODES = Map.ofEntries(
            Map.entry("중구", "26110"),
            Map.entry("서구", "26140"),
            Map.entry("동구", "26170"),
            Map.entry("영도구", "26200"),
            Map.entry("부산진구", "26230"),
            Map.entry("동래구", "26260"),
            Map.entry("남구", "26290"),
            Map.entry("북구", "26320"),
            Map.entry("해운대구", "26350"),
            Map.entry("사하구", "26380"),
            Map.entry("금정구", "26410"),
            Map.entry("강서구", "26440"),
            Map.entry("연제구", "26470"),
            Map.entry("수영구", "26500"),
            Map.entry("사상구", "26530"),
            Map.entry("기장군", "26710"));

    private final JdbcTemplate jdbcTemplate;
    private final NaverNewsSearchClient naverNewsSearchClient;
    private final OpenAiBusanRewindTrendClient openAiBusanRewindTrendClient;

    public BusanRewindService(
            JdbcTemplate jdbcTemplate,
            NaverNewsSearchClient naverNewsSearchClient,
            OpenAiBusanRewindTrendClient openAiBusanRewindTrendClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.naverNewsSearchClient = naverNewsSearchClient;
        this.openAiBusanRewindTrendClient = openAiBusanRewindTrendClient;
    }

    public ApiDataResponse<CurrentStatus> currentStatus(String industryCode) {
        IndustryScope industry = findIndustry(industryCode);
        Integer baseYear = latestSectionStatYear(industry, "EMPLOYEE_SIZE").orElse(null);
        Integer previousYear = previousSectionStatYear(industry, "EMPLOYEE_SIZE", baseYear).orElse(null);

        List<IndustryStatRow> employeeRows = baseYear == null
                ? List.of()
                : sectionStatRows(industry, baseYear, "EMPLOYEE_SIZE");
        Integer employeeCount = totalEmployeeCount(employeeRows);
        Integer previousEmployeeCount = previousYear == null
                ? null
                : totalEmployeeCount(sectionStatRows(industry, previousYear, "EMPLOYEE_SIZE"));

        List<EmployeeSizeRatio> employeeSizeRatios = employeeRows.stream()
                .filter(row -> !isTotalDimension(row.dimensionName()))
                .filter(row -> row.employeeCount() != null && employeeCount != null && employeeCount > 0)
                .map(row -> new EmployeeSizeRatio(row.dimensionName(), percent(row.employeeCount(), employeeCount)))
                .limit(8)
                .toList();

        OrganizationRatio organizationRatio =
                baseYear == null ? new OrganizationRatio(null, null) : organizationRatio(industry, baseYear);
        Integer organizationTotal = sumNullable(organizationRatio.corporationCount(), organizationRatio.individualCount());

        CurrentStatus response = new CurrentStatus(
                industry.divisionCode(),
                industry.divisionName(),
                baseYear,
                previousYear,
                new CountRatio(organizationRatio.corporationCount(), percent(organizationRatio.corporationCount(), organizationTotal)),
                new CountRatio(organizationRatio.individualCount(), percent(organizationRatio.individualCount(), organizationTotal)),
                growthRate(previousEmployeeCount, employeeCount),
                employeeCount,
                employeeSizeRatios,
                districtEmployeeGrowths(industry, baseYear, previousYear));
        return new ApiDataResponse<>(response);
    }

    public ApiDataResponse<CurrentSupportPrograms> currentSupportPrograms(String industryCode) {
        IndustryScope industry = findIndustry(industryCode);
        List<CurrentSupportProgram> items = currentSupportProgramItems(industry);
        return new ApiDataResponse<>(new CurrentSupportPrograms(industry.divisionCode(), items));
    }

    public ApiDataResponse<TrendBriefing> trendBriefing(String industryCode) {
        IndustryScope industry = findIndustry(industryCode);
        List<GrowthPoint> growthSeries = growthSeries(industry);
        Double latestGrowthRate =
                growthSeries.isEmpty() ? null : growthSeries.get(growthSeries.size() - 1).growthRate();
        List<String> fallbackPolicyKeywords = policyKeywords(industry.divisionName());
        OpenAiBusanRewindTrendClient.TrendAnalysis analysis = trendAnalysis(industry, growthSeries);
        boolean aiGenerated = analysis != null;
        List<String> domesticIssues = aiGenerated
                ? withFallback(analysis.domesticIssues(), fallbackDomesticIssues(latestGrowthRate))
                : fallbackDomesticIssues(latestGrowthRate);
        List<String> overseasIssues = aiGenerated
                ? withFallback(analysis.overseasIssues(), List.of("해외 이슈는 뉴스 분석 결과가 부족해 별도 요약하지 않습니다."))
                : List.of("해외 이슈는 네이버 뉴스/OpenAI 분석 결과가 부족해 규칙 기반으로 표시됩니다.");
        ChangeComparison changeComparison = aiGenerated
                ? analysis.changeComparison()
                : fallbackChangeComparison();
        List<String> strategicIndustries = aiGenerated
                ? withFallback(analysis.strategicIndustries(), strategicIndustries(industry.divisionName()))
                : strategicIndustries(industry.divisionName());
        List<String> policyKeywords = aiGenerated
                ? withFallback(analysis.policyKeywords(), fallbackPolicyKeywords)
                : fallbackPolicyKeywords;
        String aiSummary = aiGenerated && analysis.aiSummary() != null && !analysis.aiSummary().isBlank()
                ? analysis.aiSummary()
                : "현재 응답은 DB 성장률과 규칙 기반 키워드로 구성된 기본 요약입니다. 뉴스/OpenAI 분석 결과가 충분할 때 최신 이슈 요약으로 대체됩니다.";
        TrendBriefing response = new TrendBriefing(
                industry.divisionCode(),
                industry.divisionName(),
                new TrendDomestic(latestGrowthRate, domesticIssues),
                growthSeries,
                new TrendIssues(overseasIssues),
                changeComparison,
                new BusanRelevance(strategicIndustries, policyKeywords),
                aiGenerated ? "AI" : "RULE_BASED",
                aiSummary);
        return new ApiDataResponse<>(response);
    }

    public ApiDataResponse<SimilarFlow> similarFlow(String industryCode) {
        IndustryScope industry = findIndustry(industryCode);
        SimilarFlow response = similarFlowValue(industry);
        return new ApiDataResponse<>(response);
    }

    public ApiDataResponse<PastSupportReview> pastSupportReview(String industryCode) {
        IndustryScope industry = findIndustry(industryCode);
        SimilarFlow flow = similarFlowValue(industry);
        Period period = flow.matchedPeriod();
        List<IndustryChange> industryChanges = period == null ? List.of() : industryChanges(industry, period);
        List<PastSupportProgram> programs = period == null ? List.of() : pastSupportPrograms(industry, period);
        List<SupportedCompanyChange> companyChanges = period == null ? List.of() : supportedCompanyChanges(industry, period);

        String summary = period == null
                ? "비교 가능한 과거 시계열이 부족해 당시 산업 변화 요약을 제공하지 않습니다."
                : period.label() + " 기간에는 산업 통계 변화와 BTP 지원사업 이력이 함께 확인됩니다. 이 요약은 관찰된 변화만 정리하며 원인이나 효과를 판단하지 않습니다.";
        PastSupportReview response = new PastSupportReview(
                industry.divisionCode(), period, industryChanges, summary, programs, companyChanges);
        return new ApiDataResponse<>(response);
    }

    public ApiDataResponse<SupportComparison> supportComparison(String industryCode) {
        IndustryScope industry = findIndustry(industryCode);
        Period period = similarFlowValue(industry).matchedPeriod();
        List<String> pastFields = period == null
                ? List.of()
                : extractFields(pastSupportPrograms(industry, period).stream()
                        .map(PastSupportProgram::supportField)
                        .toList());
        List<String> currentFields = extractFields(currentSupportProgramItems(industry).stream()
                .map(CurrentSupportProgram::supportField)
                .toList());
        List<String> commonFields = currentFields.stream().filter(pastFields::contains).toList();
        List<String> newFields = currentFields.stream().filter(field -> !pastFields.contains(field)).toList();
        List<String> trendKeywords = policyKeywords(industry.divisionName());
        SupportComparison response = new SupportComparison(
                industry.divisionCode(),
                commonFields,
                pastFields,
                changedFields(pastFields, currentFields),
                currentFields,
                newFields,
                trendKeywords,
                "과거와 현재 지원사업의 분야 키워드를 비교한 참고 요약입니다. 지원사업의 적절성, 우선순위, 성과를 판단하지 않습니다.");
        return new ApiDataResponse<>(response);
    }

    private IndustryScope findIndustry(String rawIndustryCode) {
        String divisionCode = normalizeDivisionCode(rawIndustryCode);
        try {
            return jdbcTemplate.queryForObject(
                    """
                    select section_code, division_code, max(division_name) as division_name
                    from ksic_info
                    where division_code = ?
                    group by section_code, division_code
                    order by section_code
                    limit 1
                    """,
                    (rs, rowNum) -> new IndustryScope(
                            rs.getString("section_code"),
                            rs.getString("division_code"),
                            rs.getString("division_name")),
                    divisionCode);
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "지원하지 않는 산업코드입니다.");
        }
    }

    private String normalizeDivisionCode(String rawIndustryCode) {
        if (rawIndustryCode == null || rawIndustryCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "industryCode는 필수입니다.");
        }
        String normalized = rawIndustryCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() >= 3 && Character.isLetter(normalized.charAt(0))) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() > 2) {
            normalized = normalized.substring(0, 2);
        }
        if (!normalized.matches("\\d{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "industryCode는 KSIC 중분류 코드여야 합니다.");
        }
        return normalized;
    }

    private Optional<Integer> latestSectionStatYear(IndustryScope industry, String statCategory) {
        return optionalInteger(
                """
                select max(year)
                from btp_solution_industry_stat
                where section_code = ?
                  and stat_category = ?
                """,
                industry.sectionCode(),
                statCategory);
    }

    private Optional<Integer> previousSectionStatYear(IndustryScope industry, String statCategory, Integer baseYear) {
        if (baseYear == null) {
            return Optional.empty();
        }
        return optionalInteger(
                """
                select max(year)
                from btp_solution_industry_stat
                where section_code = ?
                  and stat_category = ?
                  and year < ?
                """,
                industry.sectionCode(),
                statCategory,
                baseYear);
    }

    private List<IndustryStatRow> sectionStatRows(IndustryScope industry, Integer year, String statCategory) {
        if (year == null) {
            return List.of();
        }
        return jdbcTemplate.query(
                """
                select region_name, dimension_name, establishment_count, employee_count
                from btp_solution_industry_stat
                where section_code = ?
                  and middle_industry_name = ''
                  and year = ?
                  and stat_category = ?
                """,
                (rs, rowNum) -> new IndustryStatRow(
                        rs.getString("region_name"),
                        rs.getString("dimension_name"),
                        nullableInteger(rs.getObject("establishment_count")),
                        nullableInteger(rs.getObject("employee_count"))),
                industry.sectionCode(),
                year,
                statCategory);
    }

    private Integer totalEmployeeCount(List<IndustryStatRow> rows) {
        Optional<Integer> total = rows.stream()
                .filter(row -> isTotalDimension(row.dimensionName()))
                .map(IndustryStatRow::employeeCount)
                .filter(count -> count != null && count > 0)
                .findFirst();
        if (total.isPresent()) {
            return total.get();
        }
        int sum = rows.stream()
                .filter(row -> !isTotalDimension(row.dimensionName()))
                .map(IndustryStatRow::employeeCount)
                .filter(count -> count != null && count > 0)
                .mapToInt(Integer::intValue)
                .sum();
        return sum > 0 ? sum : null;
    }

    private OrganizationRatio organizationRatio(IndustryScope industry, Integer year) {
        List<IndustryStatRow> rows = organizationStatRows(industry, year);
        int corporation = 0;
        int individual = 0;
        boolean hasOrganizationCount = false;
        for (IndustryStatRow row : rows) {
            String dimension = nullToEmpty(row.dimensionName());
            int count = row.establishmentCount() == null ? 0 : row.establishmentCount();
            String lowerDimension = dimension.toLowerCase(Locale.ROOT);
            if (dimension.contains("개인") || lowerDimension.contains("individual")) {
                individual += count;
                hasOrganizationCount = true;
            } else if ((dimension.contains("법인") && !dimension.contains("비법인"))
                    || lowerDimension.contains("corporation")) {
                corporation += count;
                hasOrganizationCount = true;
            }
        }
        if (!hasOrganizationCount) {
            return new OrganizationRatio(null, null);
        }
        return new OrganizationRatio(corporation, individual);
    }

    private List<IndustryStatRow> organizationStatRows(IndustryScope industry, Integer year) {
        if (year == null) {
            return List.of();
        }
        List<IndustryStatRow> exactRows = middleIndustryStatRows(industry, year, "ORGANIZATION_FORM", industry.divisionName());
        if (!exactRows.isEmpty()) {
            return exactRows;
        }
        String keyword = representativeIndustryKeyword(industry.divisionName());
        if (keyword.isBlank()) {
            return List.of();
        }
        return jdbcTemplate.query(
                """
                select region_name, dimension_name, establishment_count, employee_count
                from btp_solution_industry_stat
                where section_code = ?
                  and year = ?
                  and stat_category = 'ORGANIZATION_FORM'
                  and region_name = '전체'
                  and replace(middle_industry_name, ' ', '') like concat('%', replace(?, ' ', ''), '%')
                order by middle_industry_name, dimension_name
                """,
                (rs, rowNum) -> new IndustryStatRow(
                        rs.getString("region_name"),
                        rs.getString("dimension_name"),
                        nullableInteger(rs.getObject("establishment_count")),
                        nullableInteger(rs.getObject("employee_count"))),
                industry.sectionCode(),
                year,
                keyword);
    }

    private List<IndustryStatRow> middleIndustryStatRows(
            IndustryScope industry, Integer year, String statCategory, String middleIndustryName) {
        return jdbcTemplate.query(
                """
                select region_name, dimension_name, establishment_count, employee_count
                from btp_solution_industry_stat
                where section_code = ?
                  and year = ?
                  and stat_category = ?
                  and region_name = '전체'
                  and middle_industry_name = ?
                """,
                (rs, rowNum) -> new IndustryStatRow(
                        rs.getString("region_name"),
                        rs.getString("dimension_name"),
                        nullableInteger(rs.getObject("establishment_count")),
                        nullableInteger(rs.getObject("employee_count"))),
                industry.sectionCode(),
                year,
                statCategory,
                middleIndustryName);
    }

    private List<DistrictEmployeeGrowth> districtEmployeeGrowths(
            IndustryScope industry, Integer baseYear, Integer previousYear) {
        if (baseYear == null || previousYear == null) {
            return List.of();
        }
        return jdbcTemplate.query(
                        """
                        select
                            region_name,
                            sum(employee_count) filter (where year = ?) as current_employee_count,
                            sum(employee_count) filter (where year = ?) as previous_employee_count
                        from btp_solution_industry_stat
                        where section_code = ?
                          and middle_industry_name = ''
                          and stat_category = 'DISTRICT'
                          and year in (?, ?)
                          and region_name <> '전체'
                        group by region_name
                        """,
                        (rs, rowNum) -> {
                            String districtName = rs.getString("region_name");
                            Integer current = nullableInteger(rs.getObject("current_employee_count"));
                            Integer previous = nullableInteger(rs.getObject("previous_employee_count"));
                            return new DistrictEmployeeGrowth(
                                    BUSAN_SGG_CODES.get(districtName), districtName, growthRate(previous, current));
                        },
                        baseYear,
                        previousYear,
                        industry.sectionCode(),
                        baseYear,
                        previousYear)
                .stream()
                .filter(item -> item.sggCode() != null)
                .toList();
    }

    private List<CurrentSupportProgram> currentSupportProgramItems(IndustryScope industry) {
        LocalDate today = LocalDate.now();
        return jdbcTemplate.query(
                """
                select
                    p.support_program_id,
                    p.budget_program_name,
                    p.support_type,
                    p.program_category,
                    p.program_summary,
                    p.start_date,
                    p.end_date
                from btp_support_program p
                where (p.start_date is null or p.start_date <= ?)
                  and (p.end_date is null or p.end_date >= ?)
                  and exists (
                      select 1
                      from btp_support_history h
                      left join ksic_info ksic on ksic.ksic_code = h.industry_code
                      where h.code = p.code
                        and (
                            ksic.division_code = ?
                            or h.industry_code like concat(?, '%')
                        )
                  )
                order by p.end_date nulls last, p.program_year desc, p.support_program_id desc
                limit 5
                """,
                (rs, rowNum) -> toCurrentSupportProgram(
                        new CurrentSupportProgramRow(
                                rs.getLong("support_program_id"),
                                rs.getString("budget_program_name"),
                                rs.getString("support_type"),
                                rs.getString("program_category"),
                                rs.getString("program_summary"),
                                rs.getObject("start_date", LocalDate.class),
                                rs.getObject("end_date", LocalDate.class)),
                        today),
                today,
                today,
                industry.divisionCode(),
                industry.divisionCode());
    }

    private CurrentSupportProgram toCurrentSupportProgram(CurrentSupportProgramRow row, LocalDate today) {
        return new CurrentSupportProgram(
                row.programId(),
                defaultText(row.title(), "제목 없음"),
                status(row.startDate(), row.endDate(), today),
                row.endDate(),
                firstNonBlank(row.supportType(), row.programCategory(), "확인필요"),
                truncate(defaultText(row.summary(), ""), 80));
    }

    private List<GrowthPoint> growthSeries(IndustryScope industry) {
        String bokIndustryCode = bokIndustryCode(industry).orElse(industry.sectionCode() + industry.divisionCode());
        List<GrowthPoint> points = jdbcTemplate.query(
                """
                select year, value
                from industry_benchmark_metric
                where bok_industry_code = ?
                  and metric = 'REVENUE_GROWTH_RATE'
                  and value is not null
                order by year
                """,
                (rs, rowNum) -> new GrowthPoint(rs.getInt("year"), round(rs.getDouble("value"))),
                bokIndustryCode);
        if (points.size() <= 5) {
            return points;
        }
        return points.subList(points.size() - 5, points.size());
    }

    private OpenAiBusanRewindTrendClient.TrendAnalysis trendAnalysis(
            IndustryScope industry, List<GrowthPoint> growthSeries) {
        List<NaverNewsSearchClient.NaverNewsItem> newsItems =
                naverNewsSearchClient.searchIndustryNews(industry.divisionName());
        if (newsItems.isEmpty()) {
            return null;
        }
        return openAiBusanRewindTrendClient.analyze(new OpenAiBusanRewindTrendClient.TrendAnalysisRequest(
                industry.divisionCode(), industry.divisionName(), growthSeriesText(growthSeries), newsItems));
    }

    private String growthSeriesText(List<GrowthPoint> growthSeries) {
        if (growthSeries.isEmpty()) {
            return "정보 없음";
        }
        List<String> values = new ArrayList<>();
        for (GrowthPoint point : growthSeries) {
            values.add(point.year() + ":" + point.growthRate());
        }
        return String.join(", ", values);
    }

    private List<String> fallbackDomesticIssues(Double latestGrowthRate) {
        return latestGrowthRate == null
                ? List.of("성장률 DB 매핑이 없어 산업 통계 기반 확인이 필요합니다.")
                : List.of("최근 성장률 흐름을 DB 기준으로 확인했습니다.", "뉴스/OpenAI 분석 결과가 부족하면 규칙 기반 이슈를 표시합니다.");
    }

    private ChangeComparison fallbackChangeComparison() {
        return new ChangeComparison(
                "제품 변화는 지원사업 텍스트와 뉴스 이슈를 함께 분석해 요약합니다.",
                "AI, 데이터, 자동화 관련 키워드를 우선 확인합니다.",
                "부산 제조 기반과 연결되는 수요 산업을 중심으로 확인합니다.",
                "산업 구조 변화는 통계와 지원사업 분야 변화를 함께 요약합니다.");
    }

    private List<String> withFallback(List<String> values, List<String> fallback) {
        return values == null || values.isEmpty() ? fallback : values;
    }

    private Optional<String> bokIndustryCode(IndustryScope industry) {
        try {
            String value = jdbcTemplate.queryForObject(
                    """
                    select bok_industry_code
                    from ksic_bok_industry_mapping
                    where ksic_division_code = ?
                      and bok_industry_code is not null
                    order by case mapping_status when 'CONFIRMED' then 0 else 1 end, mapping_id
                    limit 1
                    """,
                    String.class,
                    industry.divisionCode());
            return Optional.ofNullable(value);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    private SimilarFlow similarFlowValue(IndustryScope industry) {
        List<YearValue> values = industryEmployeeSeries(industry);
        if (values.size() < 6) {
            return new SimilarFlow(
                    industry.divisionCode(),
                    null,
                    "데이터 부족",
                    "비교 가능한 과거 시계열이 부족합니다.",
                    List.of(),
                    List.of());
        }

        int currentWindowSize = Math.min(4, values.size() / 2);
        List<YearValue> current = values.subList(values.size() - currentWindowSize, values.size());
        Match bestMatch = bestHistoricalMatch(values, currentWindowSize);
        Period matchedPeriod = new Period(bestMatch.startYear(), bestMatch.endYear(), bestMatch.startYear() + "~" + bestMatch.endYear());
        List<IndexPoint> series = indexSeries(values, bestMatch.startYear(), bestMatch.endYear(), current.get(0).year(), current.get(current.size() - 1).year());
        List<PeriodHighlight> highlights = periodHighlights(values, matchedPeriod, current);
        String flowType = classifyFlow(current);
        return new SimilarFlow(
                industry.divisionCode(),
                matchedPeriod,
                flowType,
                current.get(0).year() + "년 이후 흐름은 " + flowType + " 유형으로 분류됩니다.",
                series,
                highlights);
    }

    private List<YearValue> industryEmployeeSeries(IndustryScope industry) {
        return jdbcTemplate.query(
                """
                select
                    year,
                    max(employee_count) filter (where dimension_name = '계') as employee_count
                from btp_solution_industry_stat
                where section_code = ?
                  and middle_industry_name = ''
                  and stat_category = 'EMPLOYEE_SIZE'
                  and employee_count is not null
                group by year
                order by year
                """,
                (rs, rowNum) -> new YearValue(rs.getInt("year"), rs.getDouble("employee_count")),
                industry.sectionCode());
    }

    private Match bestHistoricalMatch(List<YearValue> values, int windowSize) {
        int currentStart = values.size() - windowSize;
        List<Double> currentChanges = changes(values.subList(currentStart, values.size()));
        Match best = new Match(values.get(0).year(), values.get(windowSize - 1).year(), Double.MAX_VALUE);
        for (int index = 0; index + windowSize <= currentStart; index++) {
            List<YearValue> candidate = values.subList(index, index + windowSize);
            List<Double> candidateChanges = changes(candidate);
            double distance = 0;
            for (int i = 0; i < currentChanges.size(); i++) {
                double directionPenalty = Math.signum(currentChanges.get(i)) == Math.signum(candidateChanges.get(i)) ? 0 : 25;
                distance += Math.abs(currentChanges.get(i) - candidateChanges.get(i)) + directionPenalty;
            }
            if (distance < best.distance()) {
                best = new Match(candidate.get(0).year(), candidate.get(candidate.size() - 1).year(), distance);
            }
        }
        return best;
    }

    private List<Double> changes(List<YearValue> values) {
        List<Double> changes = new ArrayList<>();
        for (int i = 1; i < values.size(); i++) {
            changes.add(growthRate(values.get(i - 1).value(), values.get(i).value()));
        }
        return changes;
    }

    private List<IndexPoint> indexSeries(List<YearValue> values, int pastStart, int pastEnd, int currentStart, int currentEnd) {
        Set<Integer> selectedYears = new HashSet<>();
        for (int year = pastStart; year <= pastEnd; year++) {
            selectedYears.add(year);
        }
        for (int year = currentStart; year <= currentEnd; year++) {
            selectedYears.add(year);
        }
        Double base = values.stream()
                .filter(value -> selectedYears.contains(value.year()))
                .map(YearValue::value)
                .filter(value -> value != null && value > 0)
                .findFirst()
                .orElse(null);
        if (base == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> selectedYears.contains(value.year()))
                .map(value -> new IndexPoint(value.year(), round(value.value() / base * 100)))
                .toList();
    }

    private List<PeriodHighlight> periodHighlights(List<YearValue> values, Period matchedPeriod, List<YearValue> current) {
        List<PeriodHighlight> highlights = new ArrayList<>();
        highlights.add(new PeriodHighlight(
                "과거 유사 구간",
                matchedPeriod.startYear(),
                matchedPeriod.endYear(),
                periodChange(values, matchedPeriod.startYear(), matchedPeriod.endYear())));
        highlights.add(new PeriodHighlight(
                "최근",
                current.get(0).year(),
                current.get(current.size() - 1).year(),
                growthRate(current.get(0).value(), current.get(current.size() - 1).value())));
        return highlights;
    }

    private Double periodChange(List<YearValue> values, int startYear, int endYear) {
        Double start = values.stream().filter(value -> value.year() == startYear).map(YearValue::value).findFirst().orElse(null);
        Double end = values.stream().filter(value -> value.year() == endYear).map(YearValue::value).findFirst().orElse(null);
        return growthRate(start, end);
    }

    private String classifyFlow(List<YearValue> current) {
        List<Double> changes = changes(current);
        long upwardCount = changes.stream().filter(change -> change != null && change > 0).count();
        if (upwardCount == changes.size()) {
            double latest = changes.get(changes.size() - 1);
            double previous = changes.size() >= 2 ? changes.get(changes.size() - 2) : latest;
            if (latest >= previous * 1.5) {
                return "최근 급상승";
            }
            if (latest < previous) {
                return "상승세 둔화";
            }
            return "안정적 상승";
        }
        long downwardCount = changes.stream().filter(change -> change != null && change < 0).count();
        return downwardCount >= Math.max(1, changes.size() - 1) ? "하락세" : "혼합 흐름";
    }

    private List<IndustryChange> industryChanges(IndustryScope industry, Period period) {
        List<YearValue> values = industryEmployeeSeries(industry);
        Double employeeChange = periodChange(values, period.startYear(), period.endYear());
        return List.of(
                new IndustryChange("종사자 규모 변화", employeeChange),
                new IndustryChange("개인/법인 비중 변화", null),
                new IndustryChange("지역별 산업 분포 변화", null));
    }

    private List<PastSupportProgram> pastSupportPrograms(IndustryScope industry, Period period) {
        return jdbcTemplate.query(
                        """
                        select
                            min(p.support_program_id) as program_id,
                            coalesce(p.program_year, h.support_year) as year,
                            coalesce(p.budget_program_name, h.budget_program_name) as title,
                            max(coalesce(p.support_type, h.support_type)) as support_field,
                            max(coalesce(p.program_summary, h.support_detail, h.support_item)) as support_content,
                            sum(h.support_amount) as support_amount
                        from btp_support_history h
                        left join btp_support_program p on p.code = h.code and p.program_year = h.support_year
                        where h.support_year between ? and ?
                          and h.industry_code like concat(?, '%')
                        group by coalesce(p.program_year, h.support_year), coalesce(p.budget_program_name, h.budget_program_name)
                        order by year desc, title
                        limit 5
                        """,
                        (rs, rowNum) -> new PastSupportProgram(
                                nullableLong(rs.getObject("program_id")),
                                nullableInteger(rs.getObject("year")),
                                defaultText(rs.getString("title"), "지원사업명 없음"),
                                defaultText(rs.getString("support_content"), "지원 목적 확인 필요"),
                                "부산 소재 기업",
                                defaultText(rs.getString("support_field"), "확인필요"),
                                truncate(defaultText(rs.getString("support_content"), ""), 120),
                                nullableBigDecimal(rs.getObject("support_amount"))),
                        period.startYear(),
                        period.endYear(),
                        industry.divisionCode())
                .stream()
                .toList();
    }

    private List<SupportedCompanyChange> supportedCompanyChanges(IndustryScope industry, Period period) {
        return jdbcTemplate.query(
                        """
                        with supported as (
                            select distinct on (h.company_id)
                                h.company_id,
                                h.support_year,
                                h.main_product
                            from btp_support_history h
                            where h.company_id is not null
                              and h.support_year between ? and ?
                              and h.industry_code like concat(?, '%')
                            order by h.company_id, h.support_year
                        )
                        select
                            c.company_id,
                            c.company_name,
                            s.support_year,
                            emp_before.employee_count as employee_before,
                            emp_after.employee_count as employee_after,
                            fin_before.sales_amount as sales_before,
                            fin_after.sales_amount as sales_after,
                            s.main_product as activity_change,
                            fin_after.research_and_development_expense as rnd_after
                        from supported s
                        join company c on c.company_id = s.company_id
                        left join lateral (
                            select employee_count
                            from company_employment_statistics
                            where company_id = s.company_id and year < s.support_year
                            order by year desc
                            limit 1
                        ) emp_before on true
                        left join lateral (
                            select employee_count
                            from company_employment_statistics
                            where company_id = s.company_id and year > s.support_year
                            order by year asc
                            limit 1
                        ) emp_after on true
                        left join lateral (
                            select sales_amount
                            from company_financial_statistics
                            where company_id = s.company_id and year < s.support_year
                            order by year desc
                            limit 1
                        ) fin_before on true
                        left join lateral (
                            select sales_amount, research_and_development_expense
                            from company_financial_statistics
                            where company_id = s.company_id and year > s.support_year
                            order by year asc
                            limit 1
                        ) fin_after on true
                        order by c.company_id
                        limit 3
                        """,
                        (rs, rowNum) -> new SupportedCompanyChange(
                                rs.getInt("company_id"),
                                rs.getString("company_name"),
                                nullableInteger(rs.getObject("support_year")),
                                nullableInteger(rs.getObject("employee_before")),
                                nullableInteger(rs.getObject("employee_after")),
                                nullableBigDecimal(rs.getObject("sales_before")),
                                nullableBigDecimal(rs.getObject("sales_after")),
                                rs.getString("activity_change"),
                                rs.getObject("rnd_after") == null ? null : "지원 이후 R&D 비용 데이터 확인"),
                        period.startYear(),
                        period.endYear(),
                        industry.divisionCode());
    }

    private List<String> extractFields(List<String> rawFields) {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        for (String rawField : rawFields) {
            String field = firstNonBlank(rawField, "").trim();
            if (field.isBlank()) {
                continue;
            }
            fields.add(truncate(field, 18));
        }
        return fields.stream().limit(6).toList();
    }

    private List<ChangedField> changedFields(List<String> pastFields, List<String> currentFields) {
        if (pastFields.isEmpty() || currentFields.isEmpty()) {
            return List.of();
        }
        return List.of(new ChangedField(pastFields.get(0) + " 중심", currentFields.get(0) + " 중심"));
    }

    private List<String> policyKeywords(String industryName) {
        List<String> keywords = new ArrayList<>(List.of("AX/DX", "ESG", "지역 고용"));
        if (industryName.contains("제조") || industryName.contains("기계") || industryName.contains("장비")) {
            keywords.add(0, "스마트 제조");
            keywords.add("탄소중립");
        }
        return keywords.stream().distinct().limit(6).toList();
    }

    private List<String> strategicIndustries(String industryName) {
        if (industryName.contains("기계") || industryName.contains("장비")) {
            return List.of("첨단기계", "스마트제조");
        }
        if (industryName.contains("자동차") || industryName.contains("운송")) {
            return List.of("미래모빌리티", "스마트제조");
        }
        return List.of("스마트제조");
    }

    private String representativeIndustryKeyword(String industryName) {
        String normalized = nullToEmpty(industryName)
                .replace("제조업", "")
                .replace("및", " ")
                .replace(",", " ")
                .replace("·", " ")
                .trim();
        if (normalized.isBlank()) {
            return "";
        }
        for (String token : normalized.split("\\s+")) {
            if (token.length() >= 2 && !token.equals("기타")) {
                return token;
            }
        }
        return "";
    }

    private String status(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate != null && startDate.isAfter(today)) {
            return "예정";
        }
        if (endDate != null && endDate.isBefore(today)) {
            return "마감";
        }
        if (startDate != null || endDate != null) {
            return "접수중";
        }
        return "확인필요";
    }

    private Optional<Integer> optionalInteger(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, Integer.class, args));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    private static boolean isTotalDimension(String dimensionName) {
        String dimension = nullToEmpty(dimensionName).trim();
        return dimension.isBlank() || TOTAL_DIMENSION.equals(dimension) || dimension.contains("총계") || dimension.contains("전체");
    }

    private static Double percent(Integer numerator, Integer denominator) {
        if (numerator == null || denominator == null || denominator == 0) {
            return null;
        }
        return round(numerator * 100.0 / denominator);
    }

    private static Double growthRate(Integer previous, Integer current) {
        if (previous == null || current == null || previous == 0) {
            return null;
        }
        return round((current - previous) * 100.0 / previous);
    }

    private static Double growthRate(Double previous, Double current) {
        if (previous == null || current == null || previous == 0) {
            return null;
        }
        return round((current - previous) * 100.0 / previous);
    }

    private static Integer sumNullable(Integer left, Integer right) {
        if (left == null || right == null) {
            return null;
        }
        return left + right;
    }

    private static Double round(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static Integer nullableInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Long nullableLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static BigDecimal nullableBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record IndustryScope(String sectionCode, String divisionCode, String divisionName) {}

    private record IndustryStatRow(
            String regionName, String dimensionName, Integer establishmentCount, Integer employeeCount) {}

    private record OrganizationRatio(Integer corporationCount, Integer individualCount) {}

    private record CurrentSupportProgramRow(
            Long programId,
            String title,
            String supportType,
            String programCategory,
            String summary,
            LocalDate startDate,
            LocalDate endDate) {}

    private record YearValue(Integer year, Double value) {}

    private record Match(Integer startYear, Integer endYear, Double distance) {}
}
