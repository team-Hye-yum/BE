package site.dataon.hyeyum.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import site.dataon.hyeyum.domain.BtpSolutionIndustryStat;
import site.dataon.hyeyum.domain.KsicInfo;
import site.dataon.hyeyum.dto.ApiDataResponse;
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

    public BtpSolutionIndustryService(
            KsicInfoRepository ksicInfoRepository,
            BtpSolutionIndustryStatRepository industryStatRepository,
            BtpSolutionCompanyStatRepository companyStatRepository,
            KsicIndustryElasticsearchService elasticsearchService) {
        this.ksicInfoRepository = ksicInfoRepository;
        this.industryStatRepository = industryStatRepository;
        this.companyStatRepository = companyStatRepository;
        this.elasticsearchService = elasticsearchService;
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
}
