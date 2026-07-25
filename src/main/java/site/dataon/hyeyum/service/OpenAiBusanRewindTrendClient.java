package site.dataon.hyeyum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.dataon.hyeyum.common.error.OpenAiApiException;
import site.dataon.hyeyum.dto.BusanRewindResponses.IndustryEvidenceNews;
import site.dataon.hyeyum.dto.BusanRewindResponses.ChangeComparison;

@Component
public class OpenAiBusanRewindTrendClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAiBusanRewindTrendClient(
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String configuredApiKey,
            @Value("${openai.model:gpt-4.1-mini}") String model,
            @Value("${openai.connect-timeout-seconds:30}") long connectTimeoutSeconds,
            @Value("${openai.write-timeout-seconds:60}") long writeTimeoutSeconds,
            @Value("${openai.read-timeout-seconds:180}") long readTimeoutSeconds,
            @Value("${openai.call-timeout-seconds:240}") long callTimeoutSeconds) {
        this.httpClient =
                new OkHttpClient.Builder()
                        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                        .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                        .callTimeout(callTimeoutSeconds, TimeUnit.SECONDS)
                        .build();
        this.objectMapper = objectMapper;
        this.apiKey = resolveApiKey(configuredApiKey);
        this.model = model;
    }

    public TrendAnalysis analyze(TrendAnalysisRequest request) {
        if (apiKey == null
                || apiKey.isBlank()
                || (request.domesticNewsItems().isEmpty() && request.overseasNewsItems().isEmpty())) {
            return null;
        }
        try {
            String responseJson = postResponsesApi(request);
            String outputText = extractOutputText(objectMapper.readTree(responseJson));
            if (outputText == null || outputText.isBlank()) {
                return null;
            }
            JsonNode result = objectMapper.readTree(outputText);
            return new TrendAnalysis(
                    stringList(result.path("domesticIssues"), 3),
                    stringList(result.path("overseasIssues"), 3),
                    new ChangeComparison(
                            limited(result.path("product").asText(""), 180),
                            limited(result.path("technology").asText(""), 180),
                            limited(result.path("demand").asText(""), 180),
                            limited(result.path("structure").asText(""), 180)),
                    stringList(result.path("strategicIndustries"), 4),
                    stringList(result.path("policyKeywords"), 6),
                    limited(result.path("aiSummary").asText(""), 500));
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    public ComprehensiveBriefing comprehensiveBriefing(ComprehensiveBriefingRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            String responseJson = postComprehensiveBriefingApi(request);
            String outputText = extractOutputText(objectMapper.readTree(responseJson));
            if (outputText == null || outputText.isBlank()) {
                return null;
            }
            JsonNode result = objectMapper.readTree(outputText);
            return new ComprehensiveBriefing(
                    limited(result.path("briefingMarkdown").asText(""), 2000),
                    stringList(result.path("briefingLines"), 10),
                    limited(result.path("newsSynthesis").asText(""), 700));
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    private String postResponsesApi(TrendAnalysisRequest request) throws IOException {
        Map<String, Object> body =
                Map.of(
                        "model",
                        model,
                        "instructions",
                        """
                        부산 산업 검토 담당자를 위한 산업 트렌드 브리핑을 작성하세요.
                        입력 뉴스와 DB 성장률만 근거로 사용하고, 지원사업의 적절성/우선순위/성과는 판단하지 마세요.
                        domesticIssues는 네이버 뉴스 입력을 우선 근거로 작성하세요.
                        overseasIssues는 Google News RSS 해외 뉴스 입력을 우선 근거로 작성하세요.
                        해외 뉴스가 부족하면 해외 이슈를 무리하게 만들지 말고 빈 배열 또는 관찰된 범위만 반환하세요.
                        product, technology, demand, structure는 화면에 그대로 노출되는 실제 변화 요약입니다.
                        product, technology, demand, structure에는 "요약합니다", "확인합니다", "분석합니다", "제공됩니다" 같은 내부 처리 설명을 쓰지 마세요.
                        각 변화 요약은 완성된 한 문장으로 작성하고, 단어가 중간에 끊기지 않게 하세요.
                        모든 값은 한국어로 짧게 작성하고, JSON schema에 맞는 JSON만 반환하세요.
                        """,
                        "input",
                        new Object[] {
                            Map.of(
                                    "role",
                                    "user",
                                    "content",
                                    new Object[] {
                                        Map.of("type", "input_text", "text", prompt(request))
                                    })
                        },
                        "text",
                        Map.of("format", schema()));
        Request httpRequest =
                new Request.Builder()
                        .url("https://api.openai.com/v1/responses")
                        .header("Authorization", "Bearer " + apiKey)
                        .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON))
                        .build();
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw openAiApiException(response.code(), responseBody);
            }
            return responseBody;
        }
    }

    private String postComprehensiveBriefingApi(ComprehensiveBriefingRequest request) throws IOException {
        Map<String, Object> body =
                Map.of(
                        "model",
                        model,
                        "instructions",
                        """
                        부산 산업·지원사업 검토 담당자를 위한 'AI 종합 검토 브리핑'을 작성하세요.
                        현재 산업 현황, 과거 유사 사례, 과거/현재 지원사업 변화, RSS 뉴스 근거를 종합해 전문적으로 해석하세요.
                        briefingMarkdown은 Markdown 형식으로 작성하세요.
                        ## 현재 산업 현황, ## 과거 유사 사례, ## 지원사업 검토 관점, ## DB 검토 포인트, ## 참고 근거와 한계 소제목을 포함하세요.
                        각 소제목 아래에는 2~3개 문장 또는 bullet을 작성하고, 전체는 10줄 내외의 전문적인 브리핑으로 구성하세요.
                        DB 검토 포인트에는 기업 DB에서 우선 확인하면 좋은 항목을 안내하세요.
                        활용 가능한 DB 항목 예시는 매출액, 영업이익률, 부채비율, 자산/부채/자본, 종사자수 변화, R&D 비용, 특허·인증, 연구·활동, 과거 지원이력, 중복지원 이력입니다.
                        예시는 "부채비율이 낮은 기업을 지원 추천"처럼 선정 결론을 쓰지 말고, "부채비율과 현금흐름을 함께 확인할 필요가 있습니다"처럼 검토 지표 안내로 작성하세요.
                        briefingMarkdown에서 뉴스 근거를 사용하는 문장 끝에는 반드시 입력된 RSS 뉴스의 기사별 링크를 Markdown 형식 [기사명](URL)로 1개 이상 붙이세요.
                        Markdown 링크는 화면에서 기사명을 괄호로 감싼 형태로 표시될 예정이므로, 기사명은 짧고 출처를 식별할 수 있게 작성하세요.
                        URL은 입력된 링크를 그대로 사용하고, https://news.google.com/ 같은 루트 주소나 임의 생성 주소는 절대 사용하지 마세요.
                        briefingLines는 briefingMarkdown의 핵심 문장만 배열로 다시 제공하세요.
                        링크가 없는 내용을 단정하지 말고, 통계/지원사업 데이터 기반 참고 관점으로 표현하세요.
                        지원 여부, 선정 가능성, 평가 결과, 정책 우선순위는 판단하지 마세요.
                        마지막 문장은 반드시 참고자료 성격과 한계를 설명하세요.
                        newsSynthesis는 RSS 뉴스가 AI 브리핑의 어떤 근거 역할을 하는지 2~4문장으로 작성하세요.
                        모든 응답은 한국어 JSON만 반환하세요.
                        """,
                        "input",
                        new Object[] {
                            Map.of(
                                    "role",
                                    "user",
                                    "content",
                                    new Object[] {
                                        Map.of("type", "input_text", "text", comprehensivePrompt(request))
                                    })
                        },
                        "text",
                        Map.of("format", comprehensiveSchema()));
        Request httpRequest =
                new Request.Builder()
                        .url("https://api.openai.com/v1/responses")
                        .header("Authorization", "Bearer " + apiKey)
                        .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON))
                        .build();
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw openAiApiException(response.code(), responseBody);
            }
            return responseBody;
        }
    }

    private String prompt(TrendAnalysisRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("industryCode=").append(request.industryCode()).append('\n');
        builder.append("industryName=").append(request.industryName()).append('\n');
        builder.append("growthSeries=").append(request.growthSeriesText()).append("\n\n");
        builder.append("[국내 뉴스 - Naver]\n");
        appendNewsItems(builder, request.domesticNewsItems());
        builder.append("\n[해외 뉴스 - Google News RSS]\n");
        appendNewsItems(builder, request.overseasNewsItems());
        return builder.toString();
    }

    private String comprehensivePrompt(ComprehensiveBriefingRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("industryCode=").append(request.industryCode()).append('\n');
        builder.append("industryName=").append(request.industryName()).append('\n');
        builder.append("[현재 산업 현황]\n").append(request.currentStatusText()).append("\n\n");
        builder.append("[과거 유사 사례]\n").append(request.similarFlowText()).append("\n\n");
        builder.append("[과거 지원사업 및 지원기업 변화]\n").append(request.pastSupportReviewText()).append("\n\n");
        builder.append("[과거-현재 지원사업 비교]\n").append(request.supportComparisonText()).append("\n\n");
        builder.append("[기업 DB 검토 가능 항목]\n");
        builder.append("- 재무: 매출액, 영업이익률, 부채비율, 자산, 부채, 자본\n");
        builder.append("- 고용: 종사자수, 전년 대비 고용 증감\n");
        builder.append("- 혁신/활동: R&D 비용, 연구·활동, 특허·인증, 디지털 전환 근거\n");
        builder.append("- 지원 이력: 과거 지원사업, 중복지원 이력, 신청일자와 지원분야\n\n");
        builder.append("[산업 변화 근거 뉴스 - RSS]\n");
        if (request.evidenceNews().isEmpty()) {
            builder.append("- 수집 결과 없음\n");
        }
        for (IndustryEvidenceNews item : request.evidenceNews()) {
            builder.append("- 날짜: ").append(item.publishedAt()).append('\n');
            builder.append("  산업 변화: ").append(item.industryChange()).append('\n');
            builder.append("  제목: ").append(item.title()).append('\n');
            builder.append("  링크: ").append(item.link()).append('\n');
        }
        return builder.toString();
    }

    private void appendNewsItems(StringBuilder builder, List<NaverNewsSearchClient.NaverNewsItem> newsItems) {
        if (newsItems.isEmpty()) {
            builder.append("- 수집 결과 없음\n");
            return;
        }
        for (NaverNewsSearchClient.NaverNewsItem item : newsItems) {
            builder.append("- 제목: ").append(item.title()).append('\n');
            builder.append("  요약: ").append(item.description()).append('\n');
            builder.append("  날짜: ").append(item.publishedAt()).append('\n');
            builder.append("  링크: ").append(item.link()).append('\n');
        }
    }

    private Map<String, Object> schema() {
        Map<String, Object> stringType = Map.of("type", "string");
        Map<String, Object> stringArray =
                Map.of("type", "array", "items", stringType, "maxItems", 6);
        return Map.of(
                "type",
                "json_schema",
                "name",
                "busan_rewind_trend_analysis",
                "strict",
                true,
                "schema",
                Map.of(
                        "type",
                        "object",
                        "additionalProperties",
                        false,
                        "properties",
                        Map.of(
                                "domesticIssues",
                                stringArray,
                                "overseasIssues",
                                stringArray,
                                "product",
                                stringType,
                                "technology",
                                stringType,
                                "demand",
                                stringType,
                                "structure",
                                stringType,
                                "strategicIndustries",
                                stringArray,
                                "policyKeywords",
                                stringArray,
                                "aiSummary",
                                stringType),
                        "required",
                        new String[] {
                            "domesticIssues",
                            "overseasIssues",
                            "product",
                            "technology",
                            "demand",
                            "structure",
                            "strategicIndustries",
                            "policyKeywords",
                            "aiSummary"
                        }));
    }

    private Map<String, Object> comprehensiveSchema() {
        Map<String, Object> stringType = Map.of("type", "string");
        return Map.of(
                "type",
                "json_schema",
                "name",
                "busan_rewind_ai_review_briefing",
                "strict",
                true,
                "schema",
                Map.of(
                        "type",
                        "object",
                        "additionalProperties",
                        false,
                        "properties",
                        Map.of(
                                "briefingMarkdown",
                                stringType,
                                "briefingLines",
                                Map.of("type", "array", "items", stringType, "minItems", 8, "maxItems", 10),
                                "newsSynthesis",
                                stringType),
                        "required",
                        new String[] {"briefingMarkdown", "briefingLines", "newsSynthesis"}));
    }

    private OpenAiApiException openAiApiException(int statusCode, String responseBody) {
        try {
            JsonNode error = objectMapper.readTree(responseBody).path("error");
            String message = error.path("message").asText("OpenAI request failed.");
            String type = error.path("type").asText(null);
            String code = error.path("code").asText(null);
            return new OpenAiApiException(statusCode, message, type, code);
        } catch (IOException exception) {
            return new OpenAiApiException(statusCode, "OpenAI request failed.", null, null);
        }
    }

    private String extractOutputText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            if (node.has("output_text") && node.get("output_text").isTextual()) {
                return node.get("output_text").asText();
            }
            if (node.has("type")
                    && "output_text".equals(node.get("type").asText())
                    && node.has("text")
                    && node.get("text").isTextual()) {
                return node.get("text").asText();
            }
            Iterator<JsonNode> values = node.elements();
            while (values.hasNext()) {
                String text = extractOutputText(values.next());
                if (text != null) {
                    return text;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String text = extractOutputText(child);
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }

    private String resolveApiKey(String configuredApiKey) {
        if (configuredApiKey != null && !configuredApiKey.isBlank()) {
            return configuredApiKey;
        }
        String envApiKey = System.getenv("OPENAI_API_KEY");
        if (envApiKey != null && !envApiKey.isBlank()) {
            return envApiKey;
        }
        Path dotenv = Path.of(".env");
        if (!Files.exists(dotenv)) {
            return "";
        }
        try {
            for (String line : Files.readAllLines(dotenv, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("OPENAI_API_KEY=")) {
                    return trimmed.substring("OPENAI_API_KEY=".length()).replace("\"", "").trim();
                }
            }
        } catch (IOException ignored) {
            return "";
        }
        return "";
    }

    private List<String> stringList(JsonNode node, int limit) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String value = item.asText("");
                if (!value.isBlank()) {
                    values.add(limited(value, 120));
                }
                if (values.size() >= limit) {
                    break;
                }
            }
        }
        return values;
    }

    private String limited(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public record TrendAnalysisRequest(
            String industryCode,
            String industryName,
            String growthSeriesText,
            List<NaverNewsSearchClient.NaverNewsItem> domesticNewsItems,
            List<NaverNewsSearchClient.NaverNewsItem> overseasNewsItems) {}

    public record TrendAnalysis(
            List<String> domesticIssues,
            List<String> overseasIssues,
            ChangeComparison changeComparison,
            List<String> strategicIndustries,
            List<String> policyKeywords,
            String aiSummary) {}

    public record ComprehensiveBriefingRequest(
            String industryCode,
            String industryName,
            String currentStatusText,
            String similarFlowText,
            String pastSupportReviewText,
            String supportComparisonText,
            List<IndustryEvidenceNews> evidenceNews) {}

    public record ComprehensiveBriefing(
            String briefingMarkdown, List<String> briefingLines, String newsSynthesis) {}
}
