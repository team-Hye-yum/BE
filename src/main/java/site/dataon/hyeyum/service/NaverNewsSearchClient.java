package site.dataon.hyeyum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NaverNewsSearchClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;
    private final int display;

    public NaverNewsSearchClient(
            ObjectMapper objectMapper,
            @Value("${naver.news.client-id:}") String configuredClientId,
            @Value("${naver.news.client-secret:}") String configuredClientSecret,
            @Value("${naver.news.display:10}") int display) {
        this.httpClient =
                new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .callTimeout(30, TimeUnit.SECONDS)
                        .build();
        this.objectMapper = objectMapper;
        this.clientId = resolveValue(configuredClientId, "NAVER_NEWS_CLIENT_ID");
        this.clientSecret = resolveValue(configuredClientSecret, "NAVER_NEWS_CLIENT_SECRET");
        this.display = Math.max(1, Math.min(display, 20));
    }

    public List<NaverNewsItem> searchIndustryNews(String industryName) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return List.of();
        }
        String query = industryName + " 탄소중립 ESG AI AX 디지털 전환";
        try {
            Request request =
                    new Request.Builder()
                            .url("https://openapi.naver.com/v1/search/news.json?query="
                                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                                    + "&display="
                                    + display
                                    + "&sort=date")
                            .header("X-Naver-Client-Id", clientId)
                            .header("X-Naver-Client-Secret", clientSecret)
                            .get()
                            .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return List.of();
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                List<NaverNewsItem> items = new ArrayList<>();
                for (JsonNode item : root.path("items")) {
                    items.add(new NaverNewsItem(
                            cleanHtml(item.path("title").asText("")),
                            cleanHtml(item.path("description").asText("")),
                            item.path("originallink").asText(null),
                            item.path("pubDate").asText(null)));
                }
                return items;
            }
        } catch (IOException exception) {
            return List.of();
        }
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

    private String cleanHtml(String value) {
        return value == null ? "" : value.replaceAll("<[^>]+>", "").replace("&quot;", "\"").replace("&amp;", "&").trim();
    }

    public record NaverNewsItem(String title, String description, String link, String publishedAt) {}
}
