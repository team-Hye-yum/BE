package site.dataon.hyeyum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import site.dataon.hyeyum.common.error.OpenAiApiException;
import site.dataon.hyeyum.dto.SupportProgramAnalysisPayload;
import site.dataon.hyeyum.dto.SupportProgramPeriod;

@Component
public class OpenAiSupportProgramAnalysisClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAiSupportProgramAnalysisClient(
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

    public SupportProgramAnalysis analyze(MultipartFile pdf, int requestedYear) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is required.");
        }
        try {
            String responseJson = postResponsesApi(pdf, requestedYear);
            String outputText = extractOutputText(objectMapper.readTree(responseJson));
            if (outputText == null || outputText.isBlank()) {
                throw new IllegalStateException("OpenAI response did not contain output text.");
            }
            JsonNode analysis = objectMapper.readTree(outputText);
            return new SupportProgramAnalysis(
                    limited(requiredText(analysis, "code", fallbackCode(requestedYear)), 20),
                    limited(requiredText(analysis, "budgetProgramName", "부산TP 더미 지원사업"), 1000),
                    limited(requiredText(analysis, "programCategory", "기술개발"), 20),
                    limited(requiredText(analysis, "supportType", "사업화지원"), 20),
                    cappedDate(analysis.path("startDate").asText(null), requestedYear, Month.JANUARY, 1),
                    cappedDate(analysis.path("endDate").asText(null), requestedYear, Month.DECEMBER, 31),
                    limited(requiredText(analysis, "departmentName", "부산테크노파크"), 20),
                    limited(requiredText(analysis, "localGovernmentName", "부산광역시"), 20),
                    limited(requiredText(analysis, "programSummary", "공고문 PDF를 바탕으로 생성한 더미 지원사업입니다."), 1000));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to analyze support program PDF.", exception);
        }
    }

    public SupportProgramAnalysisPayload analyzeAnnouncement(MultipartFile pdf) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is required.");
        }
        try {
            String responseJson = postAnnouncementResponsesApi(pdf);
            String outputText = extractOutputText(objectMapper.readTree(responseJson));
            if (outputText == null || outputText.isBlank()) {
                throw new IllegalStateException("OpenAI response did not contain output text.");
            }
            JsonNode analysis = objectMapper.readTree(outputText);
            Integer programYear = analysis.path("programYear").isInt()
                    ? analysis.path("programYear").asInt()
                    : Year.now().getValue();
            LocalDate startDate = flexibleDate(analysis.path("startDate").asText(null), programYear, Month.JANUARY, 1);
            LocalDate endDate = flexibleDate(analysis.path("endDate").asText(null), programYear, Month.DECEMBER, 31);
            return new SupportProgramAnalysisPayload(
                    programYear,
                    limited(requiredText(analysis, "budgetProgramName", "부산TP 지원사업"), 1000),
                    limited(requiredText(analysis, "programCategory", "기업지원"), 20),
                    limited(requiredText(analysis, "supportType", "패키지지원"), 20),
                    new SupportProgramPeriod(formatCompactDate(startDate), formatCompactDate(endDate)),
                    limited(requiredText(analysis, "departmentName", "부산테크노파크"), 20),
                    limited(requiredText(analysis, "localGovernmentName", "부산시"), 20),
                    limited(requiredText(analysis, "programSummary", "공고문 PDF를 바탕으로 추출한 지원사업입니다."), 1000));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to analyze support program PDF.", exception);
        }
    }

    private String postResponsesApi(MultipartFile pdf, int requestedYear) throws IOException {
        String filename = pdf.getOriginalFilename() == null ? "announcement.pdf" : pdf.getOriginalFilename();
        String fileData = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdf.getBytes());
        Map<String, Object> body =
                Map.of(
                        "model",
                        model,
                        "instructions",
                        """
                        부산테크노파크 또는 지역 지원사업 공고 PDF를 분석하세요.
                        btp_support_program 테이블에 저장 가능한 필드만 반환하세요.
                        code를 제외한 모든 텍스트 값은 한국어로 작성하세요.
                        programCategory와 supportType은 20자 이내의 짧은 한국어 분류명으로 작성하세요.
                        programSummary는 한국어 한두 문장으로 간결하게 작성하세요.
                        기업 선정 여부, 합격 가능성, 추천, 자동 판단은 추론하지 마세요.
                        날짜와 연도는 requestedYear를 초과하지 않아야 합니다.
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
                                                "input_file",
                                                "filename",
                                                filename,
                                                "file_data",
                                                fileData,
                                                "detail",
                                                "low"),
                                        Map.of(
                                                "type",
                                                "input_text",
                                                "text",
                                                "requestedYear="
                                                        + requestedYear
                                                        + ". 공고문에서 한국어 지원사업 행 하나를 간결하게 추출하세요.")
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
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw openAiApiException(response.code(), responseBody);
            }
            return responseBody;
        }
    }

    private String postAnnouncementResponsesApi(MultipartFile pdf) throws IOException {
        String filename = pdf.getOriginalFilename() == null ? "announcement.pdf" : pdf.getOriginalFilename();
        String fileData = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdf.getBytes());
        Map<String, Object> body =
                Map.of(
                        "model",
                        model,
                        "instructions",
                        """
                        부산테크노파크 또는 지역 지원사업 공고 PDF를 분석하세요.
                        btp_support_program 테이블에 저장 가능한 필드만 반환하세요.
                        모든 텍스트 값은 한국어로 작성하세요.
                        programYear는 공고명, 사업명, 접수기간, 본문에 나타난 사업 기준연도를 추출하세요.
                        programCategory와 supportType은 20자 이내의 짧은 한국어 분류명으로 작성하세요.
                        startDate와 endDate는 yyyy-MM-dd 형식으로 작성하세요.
                        programSummary는 한국어 한두 문장으로 간결하게 작성하세요.
                        기업 선정 여부, 합격 가능성, 추천, 자동 판단은 추론하지 마세요.
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
                                                "input_file",
                                                "filename",
                                                filename,
                                                "file_data",
                                                fileData,
                                                "detail",
                                                "low"),
                                        Map.of(
                                                "type",
                                                "input_text",
                                                "text",
                                                "공고문에서 한국어 지원사업 행 하나를 간결하게 추출하세요. code는 반환하지 마세요.")
                                    })
                        },
                        "text",
                        Map.of("format", announcementSchema()));
        Request request =
                new Request.Builder()
                        .url("https://api.openai.com/v1/responses")
                        .header("Authorization", "Bearer " + apiKey)
                        .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON))
                        .build();
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw openAiApiException(response.code(), responseBody);
            }
            return responseBody;
        }
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

    private Map<String, Object> schema() {
        Map<String, Object> stringType = Map.of("type", "string");
        return Map.of(
                "type",
                "json_schema",
                "name",
                "btp_support_program_analysis",
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
                                "code",
                                stringType,
                                "budgetProgramName",
                                stringType,
                                "programCategory",
                                stringType,
                                "supportType",
                                stringType,
                                "startDate",
                                stringType,
                                "endDate",
                                stringType,
                                "departmentName",
                                stringType,
                                "localGovernmentName",
                                stringType,
                                "programSummary",
                                stringType),
                        "required",
                        new String[] {
                            "code",
                            "budgetProgramName",
                            "programCategory",
                            "supportType",
                            "startDate",
                            "endDate",
                            "departmentName",
                            "localGovernmentName",
                            "programSummary"
                        }));
    }

    private Map<String, Object> announcementSchema() {
        Map<String, Object> stringType = Map.of("type", "string");
        return Map.of(
                "type",
                "json_schema",
                "name",
                "btp_support_program_announcement_analysis",
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
                                "programYear",
                                Map.of("type", "integer"),
                                "budgetProgramName",
                                stringType,
                                "programCategory",
                                stringType,
                                "supportType",
                                stringType,
                                "startDate",
                                stringType,
                                "endDate",
                                stringType,
                                "departmentName",
                                stringType,
                                "localGovernmentName",
                                stringType,
                                "programSummary",
                                stringType),
                        "required",
                        new String[] {
                            "programYear",
                            "budgetProgramName",
                            "programCategory",
                            "supportType",
                            "startDate",
                            "endDate",
                            "departmentName",
                            "localGovernmentName",
                            "programSummary"
                        }));
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

    private String requiredText(JsonNode node, String fieldName, String fallback) {
        String text = node.path(fieldName).asText(null);
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text.trim();
    }

    private LocalDate cappedDate(String rawDate, int requestedYear, Month fallbackMonth, int fallbackDay) {
        try {
            LocalDate date = LocalDate.parse(rawDate);
            if (date.getYear() > requestedYear) {
                return LocalDate.of(requestedYear, date.getMonth(), Math.min(date.getDayOfMonth(), date.lengthOfMonth()));
            }
            return date;
        } catch (RuntimeException exception) {
            return LocalDate.of(requestedYear, fallbackMonth, fallbackDay);
        }
    }

    private LocalDate flexibleDate(String rawDate, int fallbackYear, Month fallbackMonth, int fallbackDay) {
        if (rawDate == null || rawDate.isBlank()) {
            return LocalDate.of(fallbackYear, fallbackMonth, fallbackDay);
        }
        try {
            if (rawDate.length() == 8 && rawDate.chars().allMatch(Character::isDigit)) {
                return LocalDate.parse(rawDate, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            }
            return LocalDate.parse(rawDate.substring(0, Math.min(rawDate.length(), 10)));
        } catch (RuntimeException exception) {
            return LocalDate.of(fallbackYear, fallbackMonth, fallbackDay);
        }
    }

    private String formatCompactDate(LocalDate date) {
        return date == null ? null : date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String fallbackCode(int year) {
        return "DUMMY" + Math.floorMod(year, 10000);
    }

    private String limited(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
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
}
