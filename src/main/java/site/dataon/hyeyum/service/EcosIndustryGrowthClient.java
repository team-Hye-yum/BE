package site.dataon.hyeyum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.dataon.hyeyum.dto.BusanRewindResponses.GrowthPoint;

@Component
public class EcosIndustryGrowthClient {

    private static final String STAT_CODE = "501Y005";
    private static final String FIRM_SIZE_ITEM_CODE = "A";
    private static final String REVENUE_GROWTH_ITEM_CODE = "506";

    /**
     * ECOS 501Y005 breaks a few sections down differently than our internal {@code bok_industry_code}
     * (e.g. section A is split into A01/A03, section D is labeled D35). These KSIC division codes have
     * a more specific ECOS item than the coarse bok_industry_code would resolve to, so they're checked
     * first. Division 02 (임업) has no dedicated ECOS series at all and is intentionally left unmapped,
     * so it falls back to the DB (which already approximates it with the combined A 농업/어업 figure).
     */
    private static final Map<String, String> DIVISION_ITEM_CODE_OVERRIDES = Map.of(
            "01", "A01",
            "03", "A03",
            "35", "D35",
            "94", "ZZZ60");

    /** Fallback overrides for coarse, multi-division bok_industry_code buckets that ECOS labels differently. */
    private static final Map<String, String> BOK_ITEM_CODE_OVERRIDES =
            Map.of("ALL", "ZZZ00", "Z", "ZZZ80", "S", "ZZZ60");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    public EcosIndustryGrowthClient(
            ObjectMapper objectMapper,
            @Value("${ecos.api-key:}") String configuredApiKey,
            @Value("${ecos.base-url:https://ecos.bok.or.kr/api}") String baseUrl) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = resolveValue(configuredApiKey, "ECOS_API_KEY");
        this.baseUrl = baseUrl;
    }

    /**
     * Fetches the annual revenue growth rate series (BOK 기업경영분석 성장성 지표, stat 501Y005) for a KSIC
     * division. Returns an empty list when the key is missing or the industry has no matching ECOS item
     * code (e.g. division 02), so callers can fall back to another source.
     */
    public List<GrowthPoint> revenueGrowthSeries(
            String ksicDivisionCode, String bokIndustryCode, int startYear, int endYear) {
        if (apiKey.isBlank()) {
            return List.of();
        }
        String industryItemCode = resolveIndustryItemCode(ksicDivisionCode, bokIndustryCode);
        if (industryItemCode == null || industryItemCode.isBlank()) {
            return List.of();
        }
        String url = "%s/StatisticSearch/%s/json/kr/1/200/%s/A/%d/%d/%s/%s/%s".formatted(
                baseUrl,
                apiKey,
                STAT_CODE,
                startYear,
                endYear,
                industryItemCode,
                FIRM_SIZE_ITEM_CODE,
                REVENUE_GROWTH_ITEM_CODE);
        try {
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return List.of();
                }
                JsonNode rows = objectMapper
                        .readTree(response.body().string())
                        .path("StatisticSearch")
                        .path("row");
                if (!rows.isArray()) {
                    return List.of();
                }
                List<GrowthPoint> points = new ArrayList<>();
                for (JsonNode row : rows) {
                    String time = row.path("TIME").asText("");
                    String dataValue = row.path("DATA_VALUE").asText("");
                    if (time.isBlank() || dataValue.isBlank()) {
                        continue;
                    }
                    try {
                        points.add(new GrowthPoint(Integer.valueOf(time), Double.valueOf(dataValue)));
                    } catch (NumberFormatException ignored) {
                        // skip malformed rows rather than failing the whole series
                    }
                }
                return points.stream().sorted(Comparator.comparing(GrowthPoint::year)).toList();
            }
        } catch (IOException exception) {
            return List.of();
        }
    }

    private String resolveIndustryItemCode(String ksicDivisionCode, String bokIndustryCode) {
        if (ksicDivisionCode != null && DIVISION_ITEM_CODE_OVERRIDES.containsKey(ksicDivisionCode)) {
            return DIVISION_ITEM_CODE_OVERRIDES.get(ksicDivisionCode);
        }
        if (bokIndustryCode == null || bokIndustryCode.isBlank()) {
            return null;
        }
        return BOK_ITEM_CODE_OVERRIDES.getOrDefault(bokIndustryCode, bokIndustryCode);
    }

    private String resolveValue(String configuredValue, String envName) {
        if (configuredValue != null && !configuredValue.isBlank()) {
            return configuredValue;
        }
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        Path dotenv = Path.of(".env");
        if (!Files.exists(dotenv)) {
            return "";
        }
        try {
            for (String line : Files.readAllLines(dotenv, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith(envName + "=")) {
                    return trimmed.substring((envName + "=").length()).replace("\"", "").trim();
                }
            }
        } catch (IOException ignored) {
            return "";
        }
        return "";
    }
}
