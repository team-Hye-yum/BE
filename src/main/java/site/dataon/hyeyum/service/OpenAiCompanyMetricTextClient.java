package site.dataon.hyeyum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import site.dataon.hyeyum.common.error.OpenAiApiException;

@Component
public class OpenAiCompanyMetricTextClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String TEMPLATE_BASE_PATH = "prompts/company-metrics";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final Path promptDirectory;

    public OpenAiCompanyMetricTextClient(
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String configuredApiKey,
            @Value("${openai.model:gpt-4.1-mini}") String model,
            @Value("${openai.connect-timeout-seconds:30}") long connectTimeoutSeconds,
            @Value("${openai.write-timeout-seconds:60}") long writeTimeoutSeconds,
            @Value("${openai.read-timeout-seconds:180}") long readTimeoutSeconds,
            @Value("${openai.call-timeout-seconds:240}") long callTimeoutSeconds,
            @Value("${openai.company-metrics.prompt-directory:}") String promptDirectory) {
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
        this.promptDirectory = promptDirectory == null || promptDirectory.isBlank()
                ? null
                : Path.of(promptDirectory);
    }

    public CompanyMetricAiText generate(CompanyMetricAiRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is required.");
        }
        try {
            String responseJson = postResponsesApi(request);
            String outputText = extractOutputText(objectMapper.readTree(responseJson));
            if (outputText == null || outputText.isBlank()) {
                throw new IllegalStateException("OpenAI response did not contain output text.");
            }
            JsonNode result = objectMapper.readTree(outputText);
            return new CompanyMetricAiText(
                    limited(requiredText(result, "aiSummary"), 4000),
                    limited(requiredText(result, "aiOneLineSummary"), 1000));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate company metric AI text.", exception);
        }
    }

    private String postResponsesApi(CompanyMetricAiRequest request) throws IOException {
        Map<String, Object> body =
                Map.of(
                        "model",
                        model,
                        "instructions",
                        "You write concise Korean business analysis text. Return only valid JSON that matches the schema.",
                        "input",
                        new Object[] {
                            Map.of(
                                    "role",
                                    "user",
                                    "content",
                                    new Object[] {
                                        Map.of("type", "input_text", "text", combinedPrompt(request))
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

    private String combinedPrompt(CompanyMetricAiRequest request) throws IOException {
        return """
                Apply each template below and generate values for the two AI columns.
                Keep each template's rules, but return the final answer using the JSON schema.

                [aiSummary template]
                %s

                [aiOneLineSummary template]
                %s
                """
                .formatted(
                        renderTemplate(loadTemplate("ai_summary.txt"), request),
                        renderTemplate(loadTemplate("ai_one_line_summary.txt"), request));
    }

    private Map<String, Object> schema() {
        Map<String, Object> stringType = Map.of("type", "string");
        return Map.of(
                "type",
                "json_schema",
                "name",
                "company_metric_ai_text",
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
                                "aiSummary",
                                stringType,
                                "aiOneLineSummary",
                                stringType),
                        "required",
                        new String[] {
                            "aiSummary",
                            "aiOneLineSummary"
                        }));
    }

    private String loadTemplate(String templateName) throws IOException {
        if (promptDirectory != null) {
            Path promptPath = promptDirectory.resolve(templateName);
            if (Files.exists(promptPath)) {
                return Files.readString(promptPath, StandardCharsets.UTF_8);
            }
        }
        ClassPathResource resource = new ClassPathResource(TEMPLATE_BASE_PATH + "/" + templateName);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String renderTemplate(String template, CompanyMetricAiRequest request) {
        Map<String, String> values =
                Map.ofEntries(
                        Map.entry("companyId", text(request.companyId())),
                        Map.entry("companyName", text(request.companyName())),
                        Map.entry("regionName", text(request.regionName())),
                        Map.entry("establishedDate", text(request.establishedDate())),
                        Map.entry("businessEntityType", text(request.businessEntityType())),
                        Map.entry("companySize", text(request.companySize())),
                        Map.entry("listingStatus", text(request.listingStatus())),
                        Map.entry("companyType", text(request.companyType())),
                        Map.entry("ksicCode", text(request.ksicCode())),
                        Map.entry("industryName", text(request.industryName())),
                        Map.entry("industryDescription", text(request.industryDescription())),
                        Map.entry("mainProduct", text(request.mainProduct())),
                        Map.entry("isClosed", text(request.isClosed())),
                        Map.entry("companyStatus", text(request.companyStatus())),
                        Map.entry("isInnobiz", text(request.isInnobiz())),
                        Map.entry("isMainbiz", text(request.isMainbiz())),
                        Map.entry("isVentureCompany", text(request.isVentureCompany())),
                        Map.entry("isMaterialsCompany", text(request.isMaterialsCompany())),
                        Map.entry("isNetCertified", text(request.isNetCertified())),
                        Map.entry("isNepCertified", text(request.isNepCertified())),
                        Map.entry("researcherCount", text(request.researcherCount())),
                        Map.entry("hasResearchLab", text(request.hasResearchLab())),
                        Map.entry("hasRndDepartment", text(request.hasRndDepartment())),
                        Map.entry("debtRatio", number(request.debtRatio())),
                        Map.entry("costOfSalesRatio", number(request.costOfSalesRatio())),
                        Map.entry("salesGrowthRate", number(request.salesGrowthRate())),
                        Map.entry("employmentGrowthRate", number(request.employmentGrowthRate())),
                        Map.entry("governmentRndDependency", number(request.governmentRndDependency())),
                        Map.entry("supportedSalesGrowthRate", number(request.supportedSalesGrowthRate())),
                        Map.entry("employmentPeakIndex", number(request.employmentPeakIndex())),
                        Map.entry("employeeTurnoverRate", number(request.employeeTurnoverRate())));
        String rendered = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
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

    private String requiredText(JsonNode node, String fieldName) {
        String text = node.path(fieldName).asText(null);
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("OpenAI response did not contain " + fieldName + ".");
        }
        return text.trim();
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

    private String limited(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String text(Object value) {
        return value == null ? "정보 없음" : value.toString();
    }

    private String number(Double value) {
        return value == null ? "정보 없음" : String.format("%.2f%%", value);
    }
}
