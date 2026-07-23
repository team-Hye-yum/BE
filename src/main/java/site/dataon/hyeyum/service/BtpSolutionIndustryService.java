package site.dataon.hyeyum.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import site.dataon.hyeyum.dto.BtpSolutionInfraHubResponse;
import site.dataon.hyeyum.dto.BtpSolutionIndustryOverviewResponse;
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
    private static final String ORGANIZATION_FORM = "ORGANIZATION_FORM";
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

    public ApiDataResponse<BtpSolutionIndustryOverviewResponse> overview(String sectionCode) {
        String normalizedSectionCode = normalizeSectionCode(sectionCode);
        KsicInfo section = ksicInfoRepository
                .findFirstBySectionCodeOrderByDivisionCodeAscGroupCodeAscClassCodeAscSubclassCodeAsc(
                        normalizedSectionCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown KSIC section code: " + sectionCode));

        Integer busanBaseYear = industryStatRepository.findLatestYearBySectionCode(normalizedSectionCode);
        Integer btpBaseYear = companyStatRepository.findLatestEmploymentYear();

        Optional<BtpSolutionIndustryStat> busanScale = busanBaseYear == null
                ? Optional.empty()
                : industryStatRepository.findBusanScale(normalizedSectionCode, busanBaseYear);
        BtpCompanyScaleProjection btpScale = btpBaseYear == null
                ? null
                : companyStatRepository.findBtpScale(normalizedSectionCode, btpBaseYear);

        BtpSolutionIndustryOverviewResponse response = new BtpSolutionIndustryOverviewResponse(
                normalizedSectionCode,
                section.getSectionName(),
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
                        busanBusinessTypeRatio(normalizedSectionCode, busanBaseYear),
                        btpBusinessTypeRatio(normalizedSectionCode)),
                employeeSizeRatios(normalizedSectionCode, busanBaseYear, btpBaseYear));

        return new ApiDataResponse<>(response);
    }

    public ApiDataResponse<BtpSolutionInfraHubResponse> infraHubs(String sectionCode) {
        String normalizedSectionCode = normalizeSectionCode(sectionCode);
        validateSectionCode(normalizedSectionCode, sectionCode);

        List<InfraHubRow> hubRows = jdbcTemplate.query(
                """
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
                    coalesce(count(distinct match.equipment_id), 0)::int as equipment_count
                from public.btp_infra_hub hub
                left join public.v_btp_equipment_hub_match match
                  on match.hub_id = hub.hub_id
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
                INFRA_HUB_ROW_MAPPER);

        Set<Long> hubIds = hubRows.stream().map(InfraHubRow::hubId).collect(Collectors.toSet());
        Map<Long, List<BtpSolutionInfraHubResponse.Facility>> facilitiesByHub = facilitiesByHub(hubIds);
        Map<Long, List<BtpSolutionInfraHubResponse.CategoryCount>> categoriesByHub = topCategoriesByHub(hubIds);
        Map<Long, List<BtpSolutionInfraHubResponse.SampleEquipment>> samplesByHub = sampleEquipmentsByHub(hubIds);

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

        return new ApiDataResponse<>(new BtpSolutionInfraHubResponse(normalizedSectionCode, hubs));
    }

    private void validateSectionCode(String normalizedSectionCode, String originalSectionCode) {
        ksicInfoRepository
                .findFirstBySectionCodeOrderByDivisionCodeAscGroupCodeAscClassCodeAscSubclassCodeAsc(
                        normalizedSectionCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown KSIC section code: " + originalSectionCode));
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

    private String normalizeSectionCode(String sectionCode) {
        String normalized = sectionCode == null ? "" : sectionCode.trim().toUpperCase();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("sectionCode must not be blank.");
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

    private Map<Long, List<BtpSolutionInfraHubResponse.CategoryCount>> topCategoriesByHub(Set<Long> hubIds) {
        if (hubIds.isEmpty()) {
            return Map.of();
        }
        return jdbcTemplate.query(
                        """
                        with category_counts as (
                            select
                                match.hub_id,
                                coalesce(equipment.category_large, '미분류') as name,
                                count(*)::int as count
                            from public.v_btp_equipment_hub_match match
                            join public.btp_equipment equipment on equipment.id = match.equipment_id
                            where match.hub_id in (%s)
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
                        hubIds.toArray())
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

    private BtpSolutionIndustryOverviewResponse.RatioPair btpBusinessTypeRatio(String sectionCode) {
        List<BtpCompanyBucketProjection> stats = companyStatRepository.findBtpBusinessTypeStats(sectionCode);
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
            String sectionCode, Integer busanBaseYear, Integer btpBaseYear) {
        Map<String, Integer> busanCounts = busanEmployeeSizeCounts(sectionCode, busanBaseYear);
        Map<String, Integer> btpCounts = btpEmployeeSizeCounts(sectionCode, btpBaseYear);
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

    private Map<String, Integer> btpEmployeeSizeCounts(String sectionCode, Integer btpBaseYear) {
        if (btpBaseYear == null) {
            return Map.of();
        }
        return bucketCounts(companyStatRepository.findBtpEmployeeSizeStats(sectionCode, btpBaseYear));
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
}
