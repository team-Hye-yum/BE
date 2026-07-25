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
        if (apiKey == null || apiKey.isBlank() || request.newsItems().isEmpty()) {
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
                            limited(result.path("product").asText(""), 120),
                            limited(result.path("technology").asText(""), 120),
                            limited(result.path("demand").asText(""), 120),
                            limited(result.path("structure").asText(""), 120)),
                    stringList(result.path("strategicIndustries"), 4),
                    stringList(result.path("policyKeywords"), 6),
                    limited(result.path("aiSummary").asText(""), 500));
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
                        국내/해외 구분이 불명확하면 무리하게 단정하지 말고 관찰된 이슈 위주로 요약하세요.
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

    private String prompt(TrendAnalysisRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("industryCode=").append(request.industryCode()).append('\n');
        builder.append("industryName=").append(request.industryName()).append('\n');
        builder.append("growthSeries=").append(request.growthSeriesText()).append("\n\n");
        builder.append("[뉴스]\n");
        for (NaverNewsSearchClient.NaverNewsItem item : request.newsItems()) {
            builder.append("- 제목: ").append(item.title()).append('\n');
            builder.append("  요약: ").append(item.description()).append('\n');
            builder.append("  날짜: ").append(item.publishedAt()).append('\n');
        }
        return builder.toString();
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
            List<NaverNewsSearchClient.NaverNewsItem> newsItems) {}

    public record TrendAnalysis(
            List<String> domesticIssues,
            List<String> overseasIssues,
            ChangeComparison changeComparison,
            List<String> strategicIndustries,
            List<String> policyKeywords,
            String aiSummary) {}
}
