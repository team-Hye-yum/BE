package site.dataon.hyeyum.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import site.dataon.hyeyum.domain.BtpSolutionIndustryStat;
import site.dataon.hyeyum.domain.KsicInfo;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.BtpSolutionConnectionEvidenceCompaniesResponse;
import site.dataon.hyeyum.dto.BtpSolutionFunctionInfraCoverageResponse;
import site.dataon.hyeyum.dto.BtpSolutionInfraConnectionMatrixResponse;
import site.dataon.hyeyum.dto.BtpSolutionInfraConnectionPositionResponse;
import site.dataon.hyeyum.dto.BtpSolutionInfraHubResponse;
import site.dataon.hyeyum.dto.BtpSolutionIndustryOverviewResponse;
import site.dataon.hyeyum.dto.BtpSolutionRelatedSupportProgramEquipmentsResponse;
import site.dataon.hyeyum.dto.BtpSolutionRelatedSupportProgramsResponse;
import site.dataon.hyeyum.dto.KsicIndustrySearchItem;
import site.dataon.hyeyum.dto.KsicIndustrySearchResponse;
import site.dataon.hyeyum.repository.BtpCompanyBucketProjection;
import site.dataon.hyeyum.repository.BtpCompanyScaleProjection;
import site.dataon.hyeyum.repository.BtpSolutionCompanyStatRepository;
import site.dataon.hyeyum.repository.BtpSolutionIndustryStatRepository;
import site.dataon.hyeyum.repository.KsicInfoRepository;
import site.dataon.hyeyum.search.KsicIndustryElasticsearchService;

@Service
public class BtpSolutionIndustryService {

    private static final Logger log = LoggerFactory.getLogger(BtpSolutionIndustryService.class);
    private static final String BUSAN_TOTAL = "부산 전체";
    private static final String EMPLOYEE_SIZE = "EMPLOYEE_SIZE";
    private static final String EMPLOYMENT_STATUS = "EMPLOYMENT_STATUS";
    private static final String ORGANIZATION_FORM = "ORGANIZATION_FORM";
    private static final int EMPLOYEE_GROWTH_START_YEAR = 2023;
    private static final int EMPLOYEE_GROWTH_END_YEAR = 2024;
    private static final List<String> EMPLOYEE_SIZE_BUCKETS =
            List.of("1~4인", "5~9인", "10~49인", "50~299인", "300인 이상");
    private static final Map<String, String> BUSAN_EMPLOYEE_SIZE_BUCKET_MAP = Map.ofEntries(
            Map.entry("1 - 4명", "1~4인"),
            Map.entry("5 - 9명", "5~9인"),
            Map.entry("10 - 19명", "10~49인"),
            Map.entry("20 - 49명", "10~49인"),
            Map.entry("50 - 99명", "50~299인"),
            Map.entry("100 - 299명", "50~299인"),
            Map.entry("300 - 499명", "300인 이상"),
            Map.entry("500 - 999명", "300인 이상"),
            Map.entry("1000명 이상", "300인 이상"));

    private final KsicInfoRepository ksicInfoRepository;
    private final BtpSolutionIndustryStatRepository industryStatRepository;
    private final BtpSolutionCompanyStatRepository companyStatRepository;
    private final KsicIndustryElasticsearchService elasticsearchService;
    private final JdbcTemplate jdbcTemplate;

    public BtpSolutionIndustryService(
            KsicInfoRepository ksicInfoRepository,
            BtpSolutionIndustryStatRepository industryStatRepository,
            BtpSolutionCompanyStatRepository companyStatRepository,
            KsicIndustryElasticsearchService elasticsearchService,
            JdbcTemplate jdbcTemplate) {
        this.ksicInfoRepository = ksicInfoRepository;
        this.industryStatRepository = industryStatRepository;
        this.companyStatRepository = companyStatRepository;
        this.elasticsearchService = elasticsearchService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ApiDataResponse<KsicIndustrySearchResponse> searchIndustries(String keyword, int limit) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        int resultLimit = Math.max(1, Math.min(limit, 50));
        if (elasticsearchService.enabled()) {
            try {
                return new ApiDataResponse<>(new KsicIndustrySearchResponse(
                        elasticsearchService.search(normalizedKeyword, resultLimit)));
            } catch (RuntimeException exception) {
                log.warn("Elasticsearch KSIC industry search failed. Falling back to database search.", exception);
            }
        }
        List<KsicIndustrySearchItem> items = ksicInfoRepository.searchByHierarchyText(normalizedKeyword).stream()
                .limit(resultLimit)
                .map(this::mapSearchItem)
                .toList();
        return new ApiDataResponse<>(new KsicIndustrySearchResponse(items));
    }

    public ApiDataResponse<BtpSolutionInfraConnectionMatrixResponse> infraConnectionMatrix() {
        List<BtpSolutionInfraConnectionMatrixResponse.Item> items = jdbcTemplate.query(
                """
                with employment as (
                    select
                        ksic.division_code,
                        ksic.division_name,
                        sum(stat.employee_count) filter (where stat.year = ?) as start_employee_count,
                        sum(stat.employee_count) filter (where stat.year = ?) as end_employee_count
                    from public.btp_solution_industry_stat stat
                    join (
                        select distinct division_code, division_name
                        from public.ksic_info
                        where division_code is not null
                    ) ksic on ksic.division_name = stat.middle_industry_name
                    where stat.stat_category = ?
                      and stat.region_name = ?
                      and stat.dimension_name = '계'
                      and stat.year in (?, ?)
                    group by ksic.division_code, ksic.division_name
                ),
                coverage as (
                %s
                )
                select
                    employment.division_code,
                    employment.division_name,
                    case
                        when employment.start_employee_count is null or employment.start_employee_count = 0
                            then null
                        else round(((employment.end_employee_count - employment.start_employee_count) * 1000.0
                            / employment.start_employee_count)) / 10.0
                    end as employee_growth_rate,
                    case
                        when coverage.detected_function_count is null or coverage.detected_function_count = 0
                            then 0.0
                        else round(coverage.connected_function_count * 1000.0
                            / coverage.detected_function_count) / 10.0
                    end as connection_rate
                from employment
                left join coverage on coverage.division_code = employment.division_code
                where employment.start_employee_count is not null
                  and employment.end_employee_count is not null
                order by employment.division_code
                """.formatted(functionInfraCoverageByDivisionSql()),
                INFRA_CONNECTION_MATRIX_ROW_MAPPER,
                EMPLOYEE_GROWTH_START_YEAR,
                EMPLOYEE_GROWTH_END_YEAR,
                EMPLOYMENT_STATUS,
                BUSAN_TOTAL,
                EMPLOYEE_GROWTH_START_YEAR,
                EMPLOYEE_GROWTH_END_YEAR);

        return new ApiDataResponse<>(new BtpSolutionInfraConnectionMatrixResponse(items));
    }

    public ApiDataResponse<BtpSolutionIndustryOverviewResponse> overview(String divisionCode) {
        KsicInfo division = findDivision(divisionCode);
        String normalizedDivisionCode = division.getDivisionCode();
        String sectionCode = division.getSectionCode();
        String industryPrefix = sectionCode + normalizedDivisionCode;

        Integer busanBaseYear = industryStatRepository.findLatestYearBySectionCode(sectionCode);
        Integer btpBaseYear = companyStatRepository.findLatestBtpEmploymentYear(industryPrefix);

        Optional<BtpSolutionIndustryStat> busanScale = busanBaseYear == null
                ? Optional.empty()
                : industryStatRepository.findBusanScale(sectionCode, busanBaseYear);
        BtpCompanyScaleProjection btpScale = btpBaseYear == null
                ? null
                : companyStatRepository.findBtpScale(industryPrefix);

        BtpSolutionIndustryOverviewResponse response = new BtpSolutionIndustryOverviewResponse(
                normalizedDivisionCode,
                division.getDivisionName(),
                busanBaseYear,
                btpBaseYear,
                new BtpSolutionIndustryOverviewResponse.IndustryScale(
                        busanScale.map(stat -> new BtpSolutionIndustryOverviewResponse.CountPair(
                                        stat.getEstablishmentCount(), stat.getEmployeeCount()))
                                .orElse(new BtpSolutionIndustryOverviewResponse.CountPair(null, null)),
                        new BtpSolutionIndustryOverviewResponse.CountPair(
                                btpScale == null ? null : btpScale.getEstablishmentCount(),
                                btpScale == null ? null : btpScale.getEmployeeCount())),
                new BtpSolutionIndustryOverviewResponse.BusinessTypeRatio(
                        busanBusinessTypeRatio(sectionCode, busanBaseYear),
                        btpBusinessTypeRatio(industryPrefix)),
                employeeSizeRatios(sectionCode, industryPrefix, busanBaseYear));

        return new ApiDataResponse<>(response);
    }

    public ApiDataResponse<BtpSolutionInfraConnectionPositionResponse> infraConnectionPosition(String divisionCode) {
        KsicInfo division = findDivision(divisionCode);
        List<IndustryPositionRow> positions = jdbcTemplate.query(
                """
                with employment as (
                    select
                        ksic.division_code,
                        ksic.division_name,
                        sum(stat.employee_count) filter (where stat.year = ?) as start_employee_count,
                        sum(stat.employee_count) filter (where stat.year = ?) as end_employee_count
                    from public.btp_solution_industry_stat stat
                    join (
                        select distinct division_code, division_name
                        from public.ksic_info
                        where division_code is not null
                    ) ksic on ksic.division_name = stat.middle_industry_name
                    where stat.stat_category = ?
                      and stat.region_name = ?
                      and stat.dimension_name = '계'
                      and stat.year in (?, ?)
                      and ksic.division_code = ?
                    group by ksic.division_code, ksic.division_name
                ),
                coverage as (
                %s
                )
                select
                    employment.division_code,
                    employment.division_name,
                    case
                        when employment.start_employee_count is null or employment.start_employee_count = 0
                            then null
                        else round(((employment.end_employee_count - employment.start_employee_count) * 1000.0
                            / employment.start_employee_count)) / 10.0
                    end as employee_growth_rate,
                    case
                        when coverage.detected_function_count is null or coverage.detected_function_count = 0
                            then 0.0
                        else round(coverage.connected_function_count * 1000.0
                            / coverage.detected_function_count) / 10.0
                    end as connection_rate
                from employment
                left join coverage on coverage.division_code = employment.division_code
                """.formatted(functionInfraCoverageByDivisionSql()),
                INDUSTRY_POSITION_ROW_MAPPER,
                EMPLOYEE_GROWTH_START_YEAR,
                EMPLOYEE_GROWTH_END_YEAR,
                EMPLOYMENT_STATUS,
                BUSAN_TOTAL,
                EMPLOYEE_GROWTH_START_YEAR,
                EMPLOYEE_GROWTH_END_YEAR,
                division.getDivisionCode());
        IndustryPositionRow position = positions.isEmpty() ? null : positions.get(0);
        BtpSolutionInfraConnectionPositionResponse response = new BtpSolutionInfraConnectionPositionResponse(
                position == null ? division.getDivisionCode() : position.divisionCode(),
                position == null ? division.getDivisionName() : position.divisionName(),
                position == null ? null : position.employeeGrowthRate(),
                position == null ? 0.0 : position.connectionRate(),
                EMPLOYEE_GROWTH_END_YEAR);

        return new ApiDataResponse<>(response);
    }

    public ApiDataResponse<BtpSolutionFunctionInfraCoverageResponse> functionInfraCoverage(String divisionCode) {
        KsicInfo division = findDivision(divisionCode);
        String normalizedDivisionCode = division.getDivisionCode();

        List<FunctionInfraCoverageRow> coverages = jdbcTemplate.query(
                """
                with coverage as (
                %s
                )
                select
                    detected_function_count,
                    connected_function_count,
                    unconnected_function_count
                from coverage
                where division_code = ?
                """.formatted(functionInfraCoverageByDivisionSql()),
                FUNCTION_INFRA_COVERAGE_ROW_MAPPER,
                normalizedDivisionCode);
        FunctionInfraCoverageRow coverage = coverages.isEmpty() ? null : coverages.get(0);

        int detectedFunctionCount = coverage == null ? 0 : coverage.detectedFunctionCount();
        int connectedFunctionCount = coverage == null ? 0 : coverage.connectedFunctionCount();
        int unconnectedFunctionCount = coverage == null ? 0 : coverage.unconnectedFunctionCount();

        BtpSolutionFunctionInfraCoverageResponse response = new BtpSolutionFunctionInfraCoverageResponse(
                normalizedDivisionCode,
                division.getDivisionName(),
                detectedFunctionCount,
                connectedFunctionCount,
                unconnectedFunctionCount,
                percentage(connectedFunctionCount, detectedFunctionCount));

        return new ApiDataResponse<>(response);
    }

    public ApiDataResponse<BtpSolutionInfraHubResponse> infraHubs(String divisionCode) {
        String normalizedDivisionCode = findDivision(divisionCode).getDivisionCode();

        List<InfraHubRow> hubRows = jdbcTemplate.query(
                evidenceCte()
                        + """
                select
                    hub.hub_id,
                    hub.hub_name,
                    hub.hub_kind,
                    hub.center_name,
                    hub.summary,
                    hub.address,
                    hub.district_name,
                    hub.tel,
                    hub.latitude,
                    hub.longitude,
                    hub.image_url,
                    hub.space_url,
                    hub.directions_url,
                    count(distinct matched.equipment_id)::int as equipment_count
                from public.btp_infra_hub hub
                join matched
                  on matched.hub_id = hub.hub_id
                where hub.active = true
                group by
                    hub.hub_id,
                    hub.hub_name,
                    hub.hub_kind,
                    hub.center_name,
                    hub.summary,
                    hub.address,
                    hub.district_name,
                    hub.tel,
                    hub.latitude,
                    hub.longitude,
                    hub.image_url,
                    hub.space_url,
                    hub.directions_url,
                    hub.display_order
                order by hub.display_order, hub.hub_id
                """,
                INFRA_HUB_ROW_MAPPER,
                normalizedDivisionCode,
                normalizedDivisionCode);

        Set<Long> hubIds = hubRows.stream().map(InfraHubRow::hubId).collect(Collectors.toSet());
        Map<Long, List<BtpSolutionInfraHubResponse.Facility>> facilitiesByHub = facilitiesByHub(hubIds);
        Map<Long, List<BtpSolutionInfraHubResponse.CategoryCount>> categoriesByHub =
                relevantTopCategoriesByHub(normalizedDivisionCode, hubIds);
        Map<Long, List<BtpSolutionInfraHubResponse.SampleEquipment>> samplesByHub =
                relevantSampleEquipmentsByHub(normalizedDivisionCode, hubIds);

        List<BtpSolutionInfraHubResponse.InfraHub> hubs = hubRows.stream()
                .map(row -> new BtpSolutionInfraHubResponse.InfraHub(
                        row.hubId(),
                        row.hubName(),
                        row.hubKind(),
                        row.centerName(),
                        row.summary(),
                        row.address(),
                        row.districtName(),
                        row.tel(),
                        row.latitude(),
                        row.longitude(),
                        row.imageUrl(),
                        row.spaceUrl(),
                        row.directionsUrl(),
                        row.equipmentCount(),
                        categoriesByHub.getOrDefault(row.hubId(), List.of()),
                        samplesByHub.getOrDefault(row.hubId(), List.of()),
                        facilitiesByHub.getOrDefault(row.hubId(), List.of())))
                .toList();

        return new ApiDataResponse<>(new BtpSolutionInfraHubResponse(normalizedDivisionCode, hubs));
    }

    public ApiDataResponse<BtpSolutionConnectionEvidenceCompaniesResponse> connectionEvidenceCompanies(
            String divisionCode, String keyword, Long hubId, int page, int size) {
        String normalizedDivisionCode = findDivision(divisionCode).getDivisionCode();
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        int pageNumber = Math.max(0, page);
        int pageSize = Math.max(1, Math.min(size, 50));
        int offset = pageNumber * pageSize;

        EvidenceSummaryRow summary = jdbcTemplate.queryForObject(
                evidenceCte()
                        + """
                        select
                            count(distinct company_id)::bigint as company_count,
                            count(distinct equipment_id)::bigint as equipment_count,
                            count(distinct hub_id)::bigint as hub_count
                        from matched
                        where (cast(? as bigint) is null or hub_id = cast(? as bigint))
                          and (? = '' or lower(company_name) like '%' || lower(?) || '%')
                        """,
                EVIDENCE_SUMMARY_ROW_MAPPER,
                normalizedDivisionCode,
                normalizedDivisionCode,
                hubId,
                hubId,
                normalizedKeyword,
                normalizedKeyword);

        List<Integer> companyIds = jdbcTemplate.query(
                evidenceCte()
                        + """
                        select company_id
                        from matched
                        where (cast(? as bigint) is null or hub_id = cast(? as bigint))
                          and (? = '' or lower(company_name) like '%' || lower(?) || '%')
                        group by company_id, company_name
                        order by company_name, company_id
                        limit ? offset ?
                        """,
                (rs, rowNum) -> rs.getInt("company_id"),
                normalizedDivisionCode,
                normalizedDivisionCode,
                hubId,
                hubId,
                normalizedKeyword,
                normalizedKeyword,
                pageSize,
                offset);

        List<BtpSolutionConnectionEvidenceCompaniesResponse.CompanyEvidenceItem> items = companyIds.isEmpty()
                ? List.of()
                : evidenceItems(normalizedDivisionCode, hubId, companyIds);
        long totalElements = summary == null ? 0 : summary.companyCount();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil(totalElements / (double) pageSize);
        BtpSolutionConnectionEvidenceCompaniesResponse.Summary responseSummary =
                new BtpSolutionConnectionEvidenceCompaniesResponse.Summary(
                        summary == null ? 0 : summary.companyCount(),
                        summary == null ? 0 : summary.equipmentCount(),
                        summary == null ? 0 : summary.hubCount());

        return new ApiDataResponse<>(new BtpSolutionConnectionEvidenceCompaniesResponse(
                normalizedDivisionCode, responseSummary, items, pageNumber, pageSize, totalElements, totalPages));
    }

    public ApiDataResponse<BtpSolutionRelatedSupportProgramsResponse> relatedSupportPrograms(String divisionCode) {
        KsicInfo division = findDivision(divisionCode);
        int equipmentCount = relatedEquipmentCount(division.getDivisionCode());
        List<BtpSolutionRelatedSupportProgramsResponse.Item> items = jdbcTemplate.query(
                """
                with selected_industry as (
                    select distinct
                        division_code,
                        division_name,
                        nullif(trim(replace(replace(division_name, '제조업', ''), '산업', '')), '') as industry_keyword
                    from public.ksic_info
                    where division_code = ?
                    limit 1
                ),
                program_text as (
                    select
                        program.support_program_id,
                        program.code,
                        program.budget_program_name,
                        program.program_year,
                        program.start_date,
                        program.end_date,
                        program.program_category,
                        program.program_summary,
                        program.support_type,
                        program.announcement_url,
                        lower(
                            coalesce(program.budget_program_name, '') || ' ' ||
                            coalesce(program.program_summary, '') || ' ' ||
                            coalesce(program.program_category, '') || ' ' ||
                            coalesce(program.support_type, '')
                        ) as normalized_text
                    from public.btp_support_program program
                )
                select
                    program.support_program_id,
                    coalesce(program.budget_program_name, program.code) as title,
                    program.program_year as year,
                    case
                        when program.start_date is null and program.end_date is null then '상시 접수중'
                        when program.start_date is not null and current_date < program.start_date then '접수예정'
                        when program.end_date is not null and current_date > program.end_date then '지원이력'
                        else '접수중'
                    end as status,
                    coalesce(industry.industry_keyword, industry.division_name) as support_field,
                    program.program_summary as support_content,
                    case
                        when program.normalized_text like '%' || lower(industry.division_name) || '%'
                            then '공고문에 산업명 직접 명시'
                        when industry.industry_keyword is not null
                         and program.normalized_text like '%' || lower(industry.industry_keyword) || '%'
                            then '지원분야에 산업 키워드 명시'
                        when program.normalized_text like '%시험%'
                          or program.normalized_text like '%분석%'
                          or program.normalized_text like '%인증%'
                          or program.normalized_text like '%장비%'
                          or program.normalized_text like '%센터%'
                          or program.normalized_text like '%인프라%'
                            then '지원내용에 장비·인프라 활용 명시'
                        else '규칙 기반 연결'
                    end as connection_basis,
                    ?::int as equipment_count,
                    program.announcement_url as announce_url
                from program_text program
                cross join selected_industry industry
                where program.normalized_text like '%' || lower(industry.division_name) || '%'
                   or (
                       industry.industry_keyword is not null
                       and length(industry.industry_keyword) >= 2
                       and program.normalized_text like '%' || lower(industry.industry_keyword) || '%'
                   )
                order by
                    program.program_year desc nulls last,
                    program.code asc,
                    program.support_program_id asc
                """,
                RELATED_SUPPORT_PROGRAM_ROW_MAPPER,
                division.getDivisionCode(),
                equipmentCount);

        return new ApiDataResponse<>(new BtpSolutionRelatedSupportProgramsResponse(items));
    }

    public ApiDataResponse<BtpSolutionRelatedSupportProgramEquipmentsResponse> relatedSupportProgramEquipments(
            String divisionCode, Long programId, int page, int size) {
        KsicInfo division = findDivision(divisionCode);
        if (programId == null || programId < 1) {
            throw new IllegalArgumentException("programId must be positive.");
        }
        boolean exists = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                """
                select exists (
                    select 1
                    from public.btp_support_program
                    where support_program_id = ?
                )
                """,
                Boolean.class,
                programId));
        if (!exists) {
            throw new IllegalArgumentException("Unknown support program id: " + programId);
        }

        int pageNumber = Math.max(0, page);
        int pageSize = Math.max(1, Math.min(size, 50));
        int offset = pageNumber * pageSize;
        long totalElements = relatedEquipmentCount(division.getDivisionCode());
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil(totalElements / (double) pageSize);
        List<BtpSolutionRelatedSupportProgramEquipmentsResponse.Item> items = jdbcTemplate.query(
                evidenceCte()
                        + """
                        , distinct_equipment as (
                            select distinct
                                equipment_id,
                                equipment_name,
                                hub_name,
                                category_large,
                                category_middle
                            from matched
                        )
                        select
                            equipment_id,
                            equipment_name,
                            hub_name,
                            category_large,
                            category_middle
                        from distinct_equipment
                        order by hub_name, equipment_name, equipment_id
                        limit ? offset ?
                        """,
                RELATED_SUPPORT_PROGRAM_EQUIPMENT_ROW_MAPPER,
                division.getDivisionCode(),
                division.getDivisionCode(),
                pageSize,
                offset);

        return new ApiDataResponse<>(new BtpSolutionRelatedSupportProgramEquipmentsResponse(
                programId, items, pageNumber, pageSize, totalElements, totalPages));
    }

    private List<BtpSolutionConnectionEvidenceCompaniesResponse.CompanyEvidenceItem> evidenceItems(
            String divisionCode, Long hubId, List<Integer> companyIds) {
        List<Object> args = new ArrayList<>();
        args.add(divisionCode);
        args.add(divisionCode);
        args.add(hubId);
        args.add(hubId);
        args.addAll(companyIds);

        List<EvidenceDetailRow> rows = jdbcTemplate.query(
                evidenceCte()
                        + """
                        select
                            company_id,
                            company_name,
                            source_field,
                            source_text,
                            keyword,
                            function_name,
                            evidence_template,
                            equipment_id,
                            equipment_name,
                            category_large,
                            hub_id,
                            hub_name
                        from matched
                        where (cast(? as bigint) is null or hub_id = cast(? as bigint))
                          and company_id in (%s)
                        order by company_name, company_id, function_name, equipment_name, hub_name
                        """
                                .formatted(placeholders(companyIds.size())),
                EVIDENCE_DETAIL_ROW_MAPPER,
                args.toArray());

        Map<Integer, CompanyEvidenceBuilder> builders = new LinkedHashMap<>();
        for (EvidenceDetailRow row : rows) {
            builders.computeIfAbsent(row.companyId(), id -> new CompanyEvidenceBuilder(id, row.companyName()))
                    .add(row);
        }
        return builders.values().stream().map(CompanyEvidenceBuilder::build).toList();
    }

    private int relatedEquipmentCount(String divisionCode) {
        Integer count = jdbcTemplate.queryForObject(
                evidenceCte()
                        + """
                        select count(distinct equipment_id)::int
                        from matched
                        """,
                Integer.class,
                divisionCode,
                divisionCode);
        return count == null ? 0 : count;
    }

    private String evidenceCte() {
        return """
                with eligible_companies as (
                    select distinct
                        company.company_id,
                        coalesce(nullif(trim(company.company_name), ''), '기업 #' || company.company_id) as company_name,
                        company.main_product
                    from public.company company
                    left join public.ksic_info company_ksic
                      on company_ksic.ksic_code = company.ksic_code
                    left join public.btp_support_history section_history
                      on section_history.company_id = company.company_id
                    left join public.ksic_info history_ksic
                      on history_ksic.ksic_code = section_history.industry_code
                    where company_ksic.division_code = ?
                       or history_ksic.division_code = ?
                ),
                company_sources as (
                    select
                        company_id,
                        company_name,
                        '주요제품' as source_field,
                        main_product as source_text
                    from eligible_companies
                    union all
                    select
                        history.company_id,
                        eligible.company_name,
                        '주요제품' as source_field,
                        history.main_product as source_text
                    from public.btp_support_history history
                    join eligible_companies eligible on eligible.company_id = history.company_id
                    union all
                    select
                        history.company_id,
                        eligible.company_name,
                        '지원품목' as source_field,
                        history.support_item as source_text
                    from public.btp_support_history history
                    join eligible_companies eligible on eligible.company_id = history.company_id
                    union all
                    select
                        project.company_id,
                        eligible.company_name,
                        'NTIS 과제명' as source_field,
                        project.project_name as source_text
                    from public.company_ntis_lead_project project
                    join eligible_companies eligible on eligible.company_id = project.company_id
                ),
                matched as (
                    select distinct
                        source.company_id,
                        source.company_name,
                        source.source_field,
                        source.source_text,
                        rule.keyword,
                        rule.function_name,
                        rule.evidence_template,
                        equipment.id as equipment_id,
                        equipment.equipment_name,
                        equipment.category_large,
                        equipment.category_middle,
                        match.hub_id,
                        hub.hub_name
                    from company_sources source
                    join public.btp_connection_keyword_rule rule
                      on rule.active = true
                     and rule.reviewed = true
                     and source.source_text is not null
                     and trim(source.source_text) <> ''
                     and lower(source.source_text) like '%' || lower(rule.keyword) || '%'
                    join public.btp_equipment equipment
                      on (rule.equipment_category_large is null
                          or equipment.category_large = rule.equipment_category_large)
                     and (rule.equipment_category_middle is null
                          or equipment.category_middle = rule.equipment_category_middle)
                    join public.v_btp_equipment_hub_match match
                      on match.equipment_id = equipment.id
                    join public.btp_infra_hub hub
                      on hub.hub_id = match.hub_id
                     and hub.active = true
                )
                """;
    }

    private KsicIndustrySearchItem mapSearchItem(KsicInfo ksicInfo) {
        return new KsicIndustrySearchItem(
                ksicInfo.getKsicCode(),
                ksicInfo.getSectionCode(),
                ksicInfo.getSectionName(),
                ksicInfo.getDivisionCode(),
                ksicInfo.getDivisionName(),
                ksicInfo.getGroupCode(),
                ksicInfo.getGroupName(),
                ksicInfo.getClassCode(),
                ksicInfo.getClassName(),
                ksicInfo.getSubclassCode(),
                ksicInfo.getSubclassName(),
                String.join(
                        " > ",
                        nullToBlank(ksicInfo.getSectionName()),
                        nullToBlank(ksicInfo.getDivisionName()),
                        nullToBlank(ksicInfo.getGroupName()),
                        nullToBlank(ksicInfo.getClassName()),
                        nullToBlank(ksicInfo.getSubclassName())));
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private KsicInfo findDivision(String divisionCode) {
        String normalizedDivisionCode = normalizeDivisionCode(divisionCode);
        return ksicInfoRepository
                .findFirstByDivisionCodeOrderByGroupCodeAscClassCodeAscSubclassCodeAsc(normalizedDivisionCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown KSIC division code: " + divisionCode));
    }

    private String normalizeDivisionCode(String divisionCode) {
        String normalized = divisionCode == null ? "" : divisionCode.trim().toUpperCase();
        if (normalized.length() == 3 && Character.isLetter(normalized.charAt(0))) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("divisionCode must not be blank.");
        }
        return normalized;
    }

    private Map<Long, List<BtpSolutionInfraHubResponse.Facility>> facilitiesByHub(Set<Long> hubIds) {
        if (hubIds.isEmpty()) {
            return Map.of();
        }
        return jdbcTemplate.query(
                        """
                        select
                            hub_id,
                            facility_id,
                            site_name,
                            building_no,
                            building_name,
                            gross_floor_area,
                            floors,
                            purpose
                        from public.btp_infra_hub_facility
                        where hub_id in (%s)
                        order by hub_id, display_order, facility_id
                        """
                                .formatted(placeholders(hubIds.size())),
                        FACILITY_ROW_MAPPER,
                        hubIds.toArray())
                .stream()
                .collect(Collectors.groupingBy(
                        FacilityRow::hubId,
                        LinkedHashMap::new,
                        Collectors.mapping(FacilityRow::facility, Collectors.toList())));
    }

    private Map<Long, List<BtpSolutionInfraHubResponse.CategoryCount>> topCategoriesByHub(
            String divisionCode, Set<Long> hubIds) {
        if (hubIds.isEmpty()) {
            return Map.of();
        }
        List<Object> args = new ArrayList<>();
        args.add(divisionCode);
        args.add(divisionCode);
        args.addAll(hubIds);
        return jdbcTemplate.query(
                        evidenceCte()
                                + """
                        , category_counts as (
                            select
                                matched.hub_id,
                                coalesce(equipment.category_large, '미분류') as name,
                                count(distinct matched.equipment_id)::int as count
                            from matched
                            where matched.hub_id in (%s)
                            group by match.hub_id, coalesce(equipment.category_large, '미분류')
                        ),
                        ranked as (
                            select
                                hub_id,
                                name,
                                count,
                                row_number() over (partition by hub_id order by count desc, name) as rank
                            from category_counts
                        )
                        select hub_id, name, count
                        from ranked
                        where rank <= 5
                        order by hub_id, rank
                        """
                                .formatted(placeholders(hubIds.size())),
                        CATEGORY_ROW_MAPPER,
                        args.toArray())
                .stream()
                .collect(Collectors.groupingBy(
                        CategoryRow::hubId,
                        LinkedHashMap::new,
                        Collectors.mapping(CategoryRow::category, Collectors.toList())));
    }

    private Map<Long, List<BtpSolutionInfraHubResponse.SampleEquipment>> sampleEquipmentsByHub(Set<Long> hubIds) {
        if (hubIds.isEmpty()) {
            return Map.of();
        }
        return jdbcTemplate.query(
                        """
                        with ranked as (
                            select
                                match.hub_id,
                                equipment.id as equipment_id,
                                equipment.equipment_name,
                                equipment.category_large,
                                equipment.location_name,
                                row_number() over (
                                    partition by match.hub_id
                                    order by equipment.id
                                ) as rank
                            from public.v_btp_equipment_hub_match match
                            join public.btp_equipment equipment on equipment.id = match.equipment_id
                            where match.hub_id in (%s)
                        )
                        select hub_id, equipment_id, equipment_name, category_large, location_name
                        from ranked
                        where rank <= 5
                        order by hub_id, rank
                        """
                                .formatted(placeholders(hubIds.size())),
                        SAMPLE_EQUIPMENT_ROW_MAPPER,
                        hubIds.toArray())
                .stream()
                .collect(Collectors.groupingBy(
                        SampleEquipmentRow::hubId,
                        LinkedHashMap::new,
                        Collectors.mapping(SampleEquipmentRow::sampleEquipment, Collectors.toList())));
    }

    private Map<Long, List<BtpSolutionInfraHubResponse.CategoryCount>> relevantTopCategoriesByHub(
            String divisionCode, Set<Long> hubIds) {
        if (hubIds.isEmpty()) {
            return Map.of();
        }
        List<Object> args = new ArrayList<>();
        args.add(divisionCode);
        args.add(divisionCode);
        args.addAll(hubIds);

        return jdbcTemplate.query(
                        evidenceCte()
                                + """
                        , category_counts as (
                            select
                                hub_id,
                                coalesce(category_large, 'Uncategorized') as name,
                                count(distinct equipment_id)::int as count
                            from matched
                            where hub_id in (%s)
                            group by hub_id, coalesce(category_large, 'Uncategorized')
                        ),
                        ranked as (
                            select
                                hub_id,
                                name,
                                count,
                                row_number() over (partition by hub_id order by count desc, name) as rank
                            from category_counts
                        )
                        select hub_id, name, count
                        from ranked
                        where rank <= 5
                        order by hub_id, rank
                        """
                                .formatted(placeholders(hubIds.size())),
                        CATEGORY_ROW_MAPPER,
                        args.toArray())
                .stream()
                .collect(Collectors.groupingBy(
                        CategoryRow::hubId,
                        LinkedHashMap::new,
                        Collectors.mapping(CategoryRow::category, Collectors.toList())));
    }

    private Map<Long, List<BtpSolutionInfraHubResponse.SampleEquipment>> relevantSampleEquipmentsByHub(
            String divisionCode, Set<Long> hubIds) {
        if (hubIds.isEmpty()) {
            return Map.of();
        }
        List<Object> args = new ArrayList<>();
        args.add(divisionCode);
        args.add(divisionCode);
        args.addAll(hubIds);

        return jdbcTemplate.query(
                        evidenceCte()
                                + """
                        , distinct_equipment as (
                            select distinct hub_id, equipment_id, equipment_name, category_large
                            from matched
                            where hub_id in (%s)
                        ),
                        ranked as (
                            select
                                distinct_equipment.hub_id,
                                distinct_equipment.equipment_id,
                                distinct_equipment.equipment_name,
                                distinct_equipment.category_large,
                                equipment.location_name,
                                row_number() over (
                                    partition by distinct_equipment.hub_id
                                    order by distinct_equipment.equipment_id
                                ) as rank
                            from distinct_equipment
                            join public.btp_equipment equipment
                              on equipment.id = distinct_equipment.equipment_id
                        )
                        select hub_id, equipment_id, equipment_name, category_large, location_name
                        from ranked
                        where rank <= 5
                        order by hub_id, rank
                        """
                                .formatted(placeholders(hubIds.size())),
                        SAMPLE_EQUIPMENT_ROW_MAPPER,
                        args.toArray())
                .stream()
                .collect(Collectors.groupingBy(
                        SampleEquipmentRow::hubId,
                        LinkedHashMap::new,
                        Collectors.mapping(SampleEquipmentRow::sampleEquipment, Collectors.toList())));
    }

    private String functionInfraCoverageByDivisionSql() {
        return """
                with sources as (
                    select
                        ksic.division_code,
                        'company.main_product' as source_field,
                        company.main_product as source_text
                    from public.company company
                    join public.ksic_info ksic on ksic.ksic_code = company.ksic_code
                    where ksic.division_code is not null
                    union all
                    select
                        ksic.division_code,
                        'history.main_product' as source_field,
                        history.main_product as source_text
                    from public.btp_support_history history
                    join public.ksic_info ksic on ksic.ksic_code = history.industry_code
                    where ksic.division_code is not null
                    union all
                    select
                        ksic.division_code,
                        'history.support_item' as source_field,
                        history.support_item as source_text
                    from public.btp_support_history history
                    join public.ksic_info ksic on ksic.ksic_code = history.industry_code
                    where ksic.division_code is not null
                    union all
                    select
                        ksic.division_code,
                        'ntis.project_name' as source_field,
                        project.project_name as source_text
                    from public.company_ntis_lead_project project
                    join public.company company on company.company_id = project.company_id
                    join public.ksic_info ksic on ksic.ksic_code = company.ksic_code
                    where ksic.division_code is not null
                ),
                cleaned as (
                    select
                        division_code,
                        source_field,
                        trim(source_text) as source_text,
                        lower(regexp_replace(trim(source_text), '\\s+', ' ', 'g')) as normalized_text
                    from sources
                    where source_text is not null
                      and trim(source_text) <> ''
                    group by
                        division_code,
                        source_field,
                        trim(source_text),
                        lower(regexp_replace(trim(source_text), '\\s+', ' ', 'g'))
                ),
                connected as (
                    select
                        source.division_code,
                        rule.function_name,
                        true as connected
                    from cleaned source
                    join public.btp_connection_keyword_rule rule
                      on rule.active = true
                     and rule.reviewed = true
                     and source.normalized_text like '%' || lower(rule.keyword) || '%'
                    join public.btp_equipment equipment
                      on (rule.equipment_category_large is null
                          or equipment.category_large = rule.equipment_category_large)
                     and (rule.equipment_category_middle is null
                          or equipment.category_middle = rule.equipment_category_middle)
                    join public.v_btp_equipment_hub_match match
                      on match.equipment_id = equipment.id
                    group by source.division_code, rule.function_name
                ),
                unconnected as (
                    select
                        source.division_code,
                        source.source_text as function_name,
                        false as connected
                    from cleaned source
                    where source.source_field in ('company.main_product', 'history.main_product')
                      and not exists (
                          select 1
                          from public.btp_connection_keyword_rule rule
                          where rule.active = true
                            and rule.reviewed = true
                            and source.normalized_text like '%' || lower(rule.keyword) || '%'
                      )
                      and length(source.normalized_text) >= 3
                      and source.normalized_text not like '%지원%'
                      and source.normalized_text not like '%사업%'
                      and source.normalized_text not like '%구축%'
                      and source.normalized_text not like '%인건비%'
                      and source.normalized_text not like '%인센티브%'
                    group by source.division_code, source.source_text
                ),
                detected as (
                    select division_code, function_name, connected from connected
                    union all
                    select division_code, function_name, connected from unconnected
                )
                select
                    division_code,
                    count(*)::int as detected_function_count,
                    count(*) filter (where connected)::int as connected_function_count,
                    count(*) filter (where not connected)::int as unconnected_function_count
                from detected
                group by division_code
                """;
    }

    private String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }

    private BtpSolutionIndustryOverviewResponse.RatioPair busanBusinessTypeRatio(
            String sectionCode, Integer busanBaseYear) {
        if (busanBaseYear == null) {
            return new BtpSolutionIndustryOverviewResponse.RatioPair(null, null);
        }
        List<BtpSolutionIndustryStat> stats =
                industryStatRepository.findBusanOrganizationStats(sectionCode, busanBaseYear);
        int total = stats.stream()
                .filter(stat -> !"계".equals(stat.getDimensionName()))
                .map(BtpSolutionIndustryStat::getEstablishmentCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        if (total == 0) {
            return new BtpSolutionIndustryOverviewResponse.RatioPair(null, null);
        }
        int corporation = stats.stream()
                .filter(stat -> containsAny(stat.getDimensionName(), "회사법인", "회사이외법인"))
                .map(BtpSolutionIndustryStat::getEstablishmentCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int individual = stats.stream()
                .filter(stat -> containsAny(stat.getDimensionName(), "개인사업체"))
                .map(BtpSolutionIndustryStat::getEstablishmentCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        return new BtpSolutionIndustryOverviewResponse.RatioPair(ratio(corporation, total), ratio(individual, total));
    }

    private BtpSolutionIndustryOverviewResponse.RatioPair btpBusinessTypeRatio(String industryPrefix) {
        List<BtpCompanyBucketProjection> stats = companyStatRepository.findBtpBusinessTypeStats(industryPrefix);
        int total = sumBuckets(stats);
        if (total == 0) {
            return new BtpSolutionIndustryOverviewResponse.RatioPair(null, null);
        }
        Map<String, Integer> counts = bucketCounts(stats);
        return new BtpSolutionIndustryOverviewResponse.RatioPair(
                ratio(counts.getOrDefault("CORPORATION", 0), total),
                ratio(counts.getOrDefault("INDIVIDUAL", 0), total));
    }

    private List<BtpSolutionIndustryOverviewResponse.EmployeeSizeRatio> employeeSizeRatios(
            String sectionCode, String industryPrefix, Integer busanBaseYear) {
        Map<String, Integer> busanCounts = busanEmployeeSizeCounts(sectionCode, busanBaseYear);
        Map<String, Integer> btpCounts = btpEmployeeSizeCounts(industryPrefix);
        int busanTotal = sumValues(busanCounts);
        int btpTotal = sumValues(btpCounts);

        return EMPLOYEE_SIZE_BUCKETS.stream()
                .map(bucket -> new BtpSolutionIndustryOverviewResponse.EmployeeSizeRatio(
                        bucket,
                        busanTotal == 0 ? null : ratio(busanCounts.getOrDefault(bucket, 0), busanTotal),
                        btpTotal == 0 ? null : ratio(btpCounts.getOrDefault(bucket, 0), btpTotal)))
                .toList();
    }

    private Map<String, Integer> busanEmployeeSizeCounts(String sectionCode, Integer busanBaseYear) {
        if (busanBaseYear == null) {
            return Map.of();
        }
        return industryStatRepository.findBusanEmployeeSizeStats(sectionCode, busanBaseYear)
                .stream()
                .filter(stat -> stat.getEstablishmentCount() != null)
                .filter(stat -> BUSAN_EMPLOYEE_SIZE_BUCKET_MAP.containsKey(stat.getDimensionName()))
                .collect(Collectors.toMap(
                        stat -> BUSAN_EMPLOYEE_SIZE_BUCKET_MAP.get(stat.getDimensionName()),
                        BtpSolutionIndustryStat::getEstablishmentCount,
                        Integer::sum));
    }

    private Map<String, Integer> btpEmployeeSizeCounts(String industryPrefix) {
        return bucketCounts(companyStatRepository.findBtpEmployeeSizeStats(industryPrefix));
    }

    private Map<String, Integer> bucketCounts(List<BtpCompanyBucketProjection> stats) {
        return stats.stream()
                .filter(stat -> stat.getName() != null)
                .filter(stat -> stat.getCount() != null)
                .collect(Collectors.toMap(
                        BtpCompanyBucketProjection::getName, BtpCompanyBucketProjection::getCount, Integer::sum));
    }

    private int sumBuckets(List<BtpCompanyBucketProjection> stats) {
        return stats.stream()
                .map(BtpCompanyBucketProjection::getCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private int sumValues(Map<String, Integer> counts) {
        return counts.values().stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    private boolean containsAny(String value, String... needles) {
        if (value == null) {
            return false;
        }
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private Double ratio(int numerator, int denominator) {
        if (denominator == 0) {
            return null;
        }
        return numerator / (double) denominator;
    }

    private Double percentage(int numerator, int denominator) {
        if (denominator == 0) {
            return null;
        }
        return Math.round(numerator * 1000.0 / denominator) / 10.0;
    }

    private static Double nullableDouble(java.sql.ResultSet rs, String columnLabel) throws java.sql.SQLException {
        double value = rs.getDouble(columnLabel);
        return rs.wasNull() ? null : value;
    }

    private static final RowMapper<InfraHubRow> INFRA_HUB_ROW_MAPPER = (rs, rowNum) -> new InfraHubRow(
            rs.getLong("hub_id"),
            rs.getString("hub_name"),
            rs.getString("hub_kind"),
            rs.getString("center_name"),
            rs.getString("summary"),
            rs.getString("address"),
            rs.getString("district_name"),
            rs.getString("tel"),
            rs.getBigDecimal("latitude"),
            rs.getBigDecimal("longitude"),
            rs.getString("image_url"),
            rs.getString("space_url"),
            rs.getString("directions_url"),
            rs.getInt("equipment_count"));

    private static final RowMapper<FacilityRow> FACILITY_ROW_MAPPER = (rs, rowNum) -> new FacilityRow(
            rs.getLong("hub_id"),
            new BtpSolutionInfraHubResponse.Facility(
                    rs.getLong("facility_id"),
                    rs.getString("site_name"),
                    rs.getString("building_no"),
                    rs.getString("building_name"),
                    rs.getString("gross_floor_area"),
                    rs.getString("floors"),
                    rs.getString("purpose")));

    private static final RowMapper<CategoryRow> CATEGORY_ROW_MAPPER = (rs, rowNum) -> new CategoryRow(
            rs.getLong("hub_id"),
            new BtpSolutionInfraHubResponse.CategoryCount(rs.getString("name"), rs.getInt("count")));

    private static final RowMapper<SampleEquipmentRow> SAMPLE_EQUIPMENT_ROW_MAPPER = (rs, rowNum) ->
            new SampleEquipmentRow(
                    rs.getLong("hub_id"),
                    new BtpSolutionInfraHubResponse.SampleEquipment(
                            rs.getLong("equipment_id"),
                            rs.getString("equipment_name"),
                            rs.getString("category_large"),
                            rs.getString("location_name")));

    private static final RowMapper<EvidenceSummaryRow> EVIDENCE_SUMMARY_ROW_MAPPER = (rs, rowNum) ->
            new EvidenceSummaryRow(
                    rs.getLong("company_count"), rs.getLong("equipment_count"), rs.getLong("hub_count"));

    private static final RowMapper<FunctionInfraCoverageRow> FUNCTION_INFRA_COVERAGE_ROW_MAPPER = (rs, rowNum) ->
            new FunctionInfraCoverageRow(
                    rs.getInt("detected_function_count"),
                    rs.getInt("connected_function_count"),
                    rs.getInt("unconnected_function_count"));

    private static final RowMapper<BtpSolutionInfraConnectionMatrixResponse.Item>
            INFRA_CONNECTION_MATRIX_ROW_MAPPER = (rs, rowNum) -> new BtpSolutionInfraConnectionMatrixResponse.Item(
                    rs.getString("division_code"),
                    rs.getString("division_name"),
                    nullableDouble(rs, "employee_growth_rate"),
                    nullableDouble(rs, "connection_rate"));

    private static final RowMapper<IndustryPositionRow> INDUSTRY_POSITION_ROW_MAPPER = (rs, rowNum) ->
            new IndustryPositionRow(
                    rs.getString("division_code"),
                    rs.getString("division_name"),
                    nullableDouble(rs, "employee_growth_rate"),
                    nullableDouble(rs, "connection_rate"));

    private static final RowMapper<BtpSolutionRelatedSupportProgramsResponse.Item>
            RELATED_SUPPORT_PROGRAM_ROW_MAPPER = (rs, rowNum) -> new BtpSolutionRelatedSupportProgramsResponse.Item(
                    rs.getLong("support_program_id"),
                    rs.getString("title"),
                    rs.getObject("year", Integer.class),
                    rs.getString("status"),
                    rs.getString("support_field"),
                    rs.getString("support_content"),
                    rs.getString("connection_basis"),
                    rs.getObject("equipment_count", Integer.class),
                    rs.getString("announce_url"));

    private static final RowMapper<BtpSolutionRelatedSupportProgramEquipmentsResponse.Item>
            RELATED_SUPPORT_PROGRAM_EQUIPMENT_ROW_MAPPER =
                    (rs, rowNum) -> new BtpSolutionRelatedSupportProgramEquipmentsResponse.Item(
                            rs.getLong("equipment_id"),
                            rs.getString("equipment_name"),
                            rs.getString("hub_name"),
                            rs.getString("category_large"),
                            rs.getString("category_middle"));

    private static final RowMapper<EvidenceDetailRow> EVIDENCE_DETAIL_ROW_MAPPER = (rs, rowNum) ->
            new EvidenceDetailRow(
                    rs.getInt("company_id"),
                    rs.getString("company_name"),
                    rs.getString("source_field"),
                    rs.getString("source_text"),
                    rs.getString("keyword"),
                    rs.getString("function_name"),
                    rs.getString("evidence_template"),
                    rs.getLong("equipment_id"),
                    rs.getString("equipment_name"),
                    rs.getString("category_large"),
                    rs.getLong("hub_id"),
                    rs.getString("hub_name"));

    private record InfraHubRow(
            Long hubId,
            String hubName,
            String hubKind,
            String centerName,
            String summary,
            String address,
            String districtName,
            String tel,
            BigDecimal latitude,
            BigDecimal longitude,
            String imageUrl,
            String spaceUrl,
            String directionsUrl,
            Integer equipmentCount) {}

    private record FacilityRow(Long hubId, BtpSolutionInfraHubResponse.Facility facility) {}

    private record CategoryRow(Long hubId, BtpSolutionInfraHubResponse.CategoryCount category) {}

    private record SampleEquipmentRow(Long hubId, BtpSolutionInfraHubResponse.SampleEquipment sampleEquipment) {}

    private record EvidenceSummaryRow(long companyCount, long equipmentCount, long hubCount) {}

    private record FunctionInfraCoverageRow(
            int detectedFunctionCount, int connectedFunctionCount, int unconnectedFunctionCount) {}

    private record IndustryPositionRow(
            String divisionCode, String divisionName, Double employeeGrowthRate, Double connectionRate) {}

    private record EvidenceDetailRow(
            Integer companyId,
            String companyName,
            String sourceField,
            String sourceText,
            String keyword,
            String functionName,
            String evidenceTemplate,
            Long equipmentId,
            String equipmentName,
            String categoryLarge,
            Long hubId,
            String hubName) {}

    private static final class CompanyEvidenceBuilder {

        private final Integer companyId;
        private final String companyName;
        private final LinkedHashSet<String> mainProducts = new LinkedHashSet<>();
        private final LinkedHashSet<String> connectedFunctions = new LinkedHashSet<>();
        private final LinkedHashMap<Long, BtpSolutionConnectionEvidenceCompaniesResponse.ConnectedEquipment>
                connectedEquipments = new LinkedHashMap<>();
        private String evidenceText;

        private CompanyEvidenceBuilder(Integer companyId, String companyName) {
            this.companyId = companyId;
            this.companyName = companyName;
        }

        private CompanyEvidenceBuilder add(EvidenceDetailRow row) {
            if (("주요제품".equals(row.sourceField()) || "지원품목".equals(row.sourceField()))
                    && row.sourceText() != null
                    && !row.sourceText().isBlank()) {
                mainProducts.add(row.sourceText().trim());
            }
            if (row.functionName() != null && !row.functionName().isBlank()) {
                connectedFunctions.add(row.functionName());
            }
            if (connectedEquipments.size() < 5 && !connectedEquipments.containsKey(row.equipmentId())) {
                connectedEquipments.put(
                        row.equipmentId(),
                        new BtpSolutionConnectionEvidenceCompaniesResponse.ConnectedEquipment(
                                row.equipmentId(),
                                row.equipmentName(),
                                row.categoryLarge(),
                                row.hubId(),
                                row.hubName()));
            }
            if (evidenceText == null) {
                evidenceText = evidenceText(row);
            }
            return this;
        }

        private BtpSolutionConnectionEvidenceCompaniesResponse.CompanyEvidenceItem build() {
            return new BtpSolutionConnectionEvidenceCompaniesResponse.CompanyEvidenceItem(
                    companyId,
                    companyName,
                    mainProducts.stream().limit(4).toList(),
                    connectedFunctions.stream().limit(5).toList(),
                    connectedEquipments.values().stream().toList(),
                    evidenceText);
        }

        private String evidenceText(EvidenceDetailRow row) {
            String template = row.evidenceTemplate() == null || row.evidenceTemplate().isBlank()
                    ? "{sourceField} '{keyword}' 표현에서 {functionName} 수요 확인"
                    : row.evidenceTemplate();
            return template.replace("{sourceField}", row.sourceField())
                    .replace("{keyword}", row.keyword())
                    .replace("{functionName}", row.functionName());
        }
    }
}
