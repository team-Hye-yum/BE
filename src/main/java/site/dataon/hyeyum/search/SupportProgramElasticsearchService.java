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
import site.dataon.hyeyum.domain.BtpSupportProgram;
import site.dataon.hyeyum.dto.SupportProgramPeriod;
import site.dataon.hyeyum.dto.SupportProgramSearchItem;

@Service
public class SupportProgramElasticsearchService {

    private static final Logger log = LoggerFactory.getLogger(SupportProgramElasticsearchService.class);
    private static final IndexCoordinates SUPPORT_PROGRAM_INDEX = IndexCoordinates.of("support-programs");

    private final ElasticsearchOperations operations;
    private final ElasticsearchClient client;
    private final SupportProgramSearchProperties properties;

    public SupportProgramElasticsearchService(
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

    public List<SupportProgramSearchItem> search(String keyword, int limit) {
        String normalized = KoreanSearchText.normalizedText(keyword);
        String chosung = KoreanSearchText.chosung(normalized);
        String jamo = KoreanSearchText.jamo(normalized);
        try {
            SearchResponse<Map> response = client.search(search -> search
                    .index("support-programs")
                    .size(Math.max(1, limit))
                    .query(query -> query.bool(bool -> bool
                            .minimumShouldMatch("1")
                            .should(should -> should.term(term -> term
                                    .field("code")
                                    .value(normalized)
                                    .boost(8.0f)))
                            .should(should -> should.match(match -> match
                                    .field("budgetProgramName")
                                    .query(normalized)
                                    .boost(5.0f)))
                            .should(should -> should.match(match -> match
                                    .field("searchText")
                                    .query(normalized)
                                    .boost(4.0f)))
                            .should(should -> should.match(match -> match
                                    .field("searchChosung")
                                    .query(chosung)
                                    .boost(3.0f)))
                            .should(should -> should.match(match -> match
                                    .field("searchJamo")
                                    .query(jamo)
                                    .boost(2.0f)))
                            .should(should -> should.match(match -> match
                                    .field("programSummary")
                                    .query(normalized)
                                    .boost(1.0f)))))
                    .sort(sort -> sort.field(field -> field.field("programYear").order(SortOrder.Desc)))
                    .sort(sort -> sort.field(field -> field.field("code").order(SortOrder.Asc))), Map.class);
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .map(this::mapSearchItem)
                    .toList();
        } catch (RuntimeException | java.io.IOException exception) {
            throw new IllegalStateException("Elasticsearch support program search failed.", exception);
        }
    }

    public void index(BtpSupportProgram program) {
        if (!enabled() || program.getSupportProgramId() == null) {
            return;
        }
        try {
            ensureIndex();
            operations.save(SupportProgramSearchDocument.from(program), SUPPORT_PROGRAM_INDEX);
        } catch (RuntimeException exception) {
            log.warn("Failed to index support program in Elasticsearch. id={}", program.getSupportProgramId(), exception);
        }
    }

    public boolean reindexAll(Collection<BtpSupportProgram> programs) {
        if (!enabled()) {
            return true;
        }
        try {
            ensureIndex();
            operations.save(programs.stream()
                    .filter(program -> program.getSupportProgramId() != null)
                    .map(SupportProgramSearchDocument::from)
                    .toList(), SUPPORT_PROGRAM_INDEX);
            return true;
        } catch (RuntimeException exception) {
            log.info("Elasticsearch is not ready for support program sync yet. reason={}", exception.getMessage());
            return false;
        }
    }

    private void ensureIndex() {
        IndexOperations indexOperations = operations.indexOps(SupportProgramSearchDocument.class);
        if (!indexOperations.exists()) {
            indexOperations.createWithMapping();
        }
    }

    private SupportProgramSearchItem mapSearchItem(Map<String, Object> source) {
        return new SupportProgramSearchItem(
                text(source, "code"),
                integer(source, "programYear"),
                text(source, "budgetProgramName"),
                text(source, "programCategory"),
                text(source, "supportType"),
                new SupportProgramPeriod(text(source, "startDate"), text(source, "endDate")),
                text(source, "departmentName"),
                text(source, "localGovernmentName"));
    }

    private String text(Map<String, Object> source, String field) {
        Object value = source == null ? null : source.get(field);
        return value == null ? null : value.toString();
    }

    private Integer integer(Map<String, Object> source, String field) {
        Object value = source == null ? null : source.get(field);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Integer.valueOf(value.toString());
    }
}
