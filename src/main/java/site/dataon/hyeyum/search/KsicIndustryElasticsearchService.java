package site.dataon.hyeyum.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import site.dataon.hyeyum.domain.KsicInfo;
import site.dataon.hyeyum.dto.KsicIndustrySearchItem;

@Service
public class KsicIndustryElasticsearchService {

    private static final Logger log = LoggerFactory.getLogger(KsicIndustryElasticsearchService.class);
    private static final IndexCoordinates KSIC_INDUSTRY_INDEX = IndexCoordinates.of("ksic-industries");

    private final ElasticsearchOperations operations;
    private final ElasticsearchClient client;
    private final SupportProgramSearchProperties properties;

    public KsicIndustryElasticsearchService(
            ElasticsearchOperations operations,
            ElasticsearchClient client,
            SupportProgramSearchProperties properties) {
        this.operations = operations;
        this.client = client;
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public List<KsicIndustrySearchItem> search(String keyword, int limit) {
        String normalized = KoreanSearchText.normalizedText(keyword);
        String chosung = KoreanSearchText.chosung(normalized);
        String jamo = KoreanSearchText.jamo(normalized);
        try {
            SearchResponse<Map> response = client.search(search -> search
                    .index("ksic-industries")
                    .size(Math.max(1, limit))
                    .query(query -> query.bool(bool -> bool.minimumShouldMatch("1")
                            .should(should -> should.term(term -> term
                                    .field("ksicCode")
                                    .value(normalized.toUpperCase())
                                    .boost(10.0f)))
                            .should(should -> should.term(term -> term
                                    .field("subclassCode")
                                    .value(normalized)
                                    .boost(9.0f)))
                            .should(should -> should.match(match -> match
                                    .field("subclassName")
                                    .query(normalized)
                                    .boost(7.0f)))
                            .should(should -> should.match(match -> match
                                    .field("groupName")
                                    .query(normalized)
                                    .boost(6.0f)))
                            .should(should -> should.match(match -> match
                                    .field("className")
                                    .query(normalized)
                                    .boost(5.0f)))
                            .should(should -> should.match(match -> match
                                    .field("divisionName")
                                    .query(normalized)
                                    .boost(4.0f)))
                            .should(should -> should.match(match -> match
                                    .field("sectionName")
                                    .query(normalized)
                                    .boost(3.0f)))
                            .should(should -> should.match(match -> match
                                    .field("searchText")
                                    .query(normalized)
                                    .boost(3.0f)))
                            .should(should -> should.match(match -> match
                                    .field("searchChosung")
                                    .query(chosung)
                                    .boost(2.0f)))
                            .should(should -> should.match(match -> match
                                    .field("searchJamo")
                                    .query(jamo)
                                    .boost(1.0f)))))
                    .sort(sort -> sort.field(field -> field.field("sectionCode").order(SortOrder.Asc)))
                    .sort(sort -> sort.field(field -> field.field("divisionCode").order(SortOrder.Asc)))
                    .sort(sort -> sort.field(field -> field.field("groupCode").order(SortOrder.Asc)))
                    .sort(sort -> sort.field(field -> field.field("classCode").order(SortOrder.Asc)))
                    .sort(sort -> sort.field(field -> field.field("subclassCode").order(SortOrder.Asc))), Map.class);
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .map(this::mapSearchItem)
                    .toList();
        } catch (RuntimeException | java.io.IOException exception) {
            throw new IllegalStateException("Elasticsearch KSIC industry search failed.", exception);
        }
    }

    public boolean reindexAll(Collection<KsicInfo> ksicInfos) {
        if (!enabled()) {
            return true;
        }
        try {
            ensureIndex();
            operations.save(ksicInfos.stream()
                    .filter(ksicInfo -> ksicInfo.getKsicCode() != null)
                    .map(KsicIndustrySearchDocument::from)
                    .toList(), KSIC_INDUSTRY_INDEX);
            return true;
        } catch (RuntimeException exception) {
            log.info("Elasticsearch is not ready for KSIC industry sync yet. reason={}", exception.getMessage());
            return false;
        }
    }

    private void ensureIndex() {
        IndexOperations indexOperations = operations.indexOps(KsicIndustrySearchDocument.class);
        if (!indexOperations.exists()) {
            indexOperations.createWithMapping();
        }
    }

    private KsicIndustrySearchItem mapSearchItem(Map<String, Object> source) {
        return new KsicIndustrySearchItem(
                text(source, "ksicCode"),
                text(source, "sectionCode"),
                text(source, "sectionName"),
                text(source, "divisionCode"),
                text(source, "divisionName"),
                text(source, "groupCode"),
                text(source, "groupName"),
                text(source, "classCode"),
                text(source, "className"),
                text(source, "subclassCode"),
                text(source, "subclassName"),
                text(source, "displayName"));
    }

    private String text(Map<String, Object> source, String field) {
        Object value = source == null ? null : source.get(field);
        return value == null ? null : value.toString();
    }
}
