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

@Component
public class OpenAiIndustryKeywordClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAiIndustryKeywordClient(
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

    public IndustryNewsKeywords generate(String industryCode, String industryName) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> body =
                    Map.of(
                            "model",
                            model,
                            "instructions",
                            """
                            KSIC 산업명은 통계 분류명이라 뉴스 검색어로 부적합할 수 있습니다.
                            네이버 뉴스에서 실제 기사 제목/본문에 등장할 가능성이 높은 한국어 검색 키워드를 작성하세요.
                            너무 넓은 단어만 단독으로 쓰지 말고, 제품/기술/시장/정책 맥락이 드러나는 짧은 명사구를 사용하세요.
                            부산 지역성과 연결 가능한 검색어는 별도 배열에 작성하세요.
                            JSON schema에 맞는 JSON만 반환하세요.
                            """,
                            "input",
                            new Object[] {
                                Map.of(
                                        "role",
                                        "user",
                                        "content",
                                        new Object[] {
                                            Map.of(
                                                    "type",
                                                    "input_text",
                                                    "text",
                                                    "industryCode=" + industryCode + "\nindustryName=" + industryName)
                                        })
                            },
                            "text",
                            Map.of("format", schema()));
            Request request =
                    new Request.Builder()
                            .url("https://api.openai.com/v1/responses")
                            .header("Authorization", "Bearer " + apiKey)
                            .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON))
                            .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return null;
                }
                String outputText = extractOutputText(objectMapper.readTree(response.body().string()));
                if (outputText == null || outputText.isBlank()) {
                    return null;
                }
                JsonNode root = objectMapper.readTree(outputText);
                return new IndustryNewsKeywords(
                        stringList(root.path("primaryKeywords"), 5),
                        stringList(root.path("trendKeywords"), 5),
                        stringList(root.path("busanKeywords"), 3));
            }
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    private Map<String, Object> schema() {
        Map<String, Object> stringArray = Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 5);
        return Map.of(
                "type",
                "json_schema",
                "name",
                "industry_news_keywords",
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
                                "primaryKeywords",
                                stringArray,
                                "trendKeywords",
                                stringArray,
                                "busanKeywords",
                                Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 3)),
                        "required",
                        new String[] {"primaryKeywords", "trendKeywords", "busanKeywords"}));
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
                String value = item.asText("").trim();
                if (!value.isBlank()) {
                    values.add(value.length() > 40 ? value.substring(0, 40) : value);
                }
                if (values.size() >= limit) {
                    break;
                }
            }
        }
        return values;
    }

    public record IndustryNewsKeywords(
            List<String> primaryKeywords, List<String> trendKeywords, List<String> busanKeywords) {}
}
