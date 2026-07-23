package site.dataon.hyeyum.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import site.dataon.hyeyum.domain.KsicInfo;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.KsicIndustrySearchItem;
import site.dataon.hyeyum.dto.KsicIndustrySearchResponse;
import site.dataon.hyeyum.repository.KsicInfoRepository;
import site.dataon.hyeyum.search.KsicIndustryElasticsearchService;

@Service
public class BtpSolutionIndustryService {

    private static final Logger log = LoggerFactory.getLogger(BtpSolutionIndustryService.class);

    private final KsicInfoRepository ksicInfoRepository;
    private final KsicIndustryElasticsearchService elasticsearchService;

    public BtpSolutionIndustryService(
            KsicInfoRepository ksicInfoRepository,
            KsicIndustryElasticsearchService elasticsearchService) {
        this.ksicInfoRepository = ksicInfoRepository;
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
}
