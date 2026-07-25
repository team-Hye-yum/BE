package site.dataon.hyeyum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        return searchIndustryNews(industryName, null);
    }

    public List<NaverNewsItem> searchIndustryNews(
            String industryName, OpenAiIndustryKeywordClient.IndustryNewsKeywords keywords) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return List.of();
        }
        Map<String, NaverNewsItem> uniqueItems = new LinkedHashMap<>();
        for (String query : newsQueries(industryName, keywords)) {
            for (NaverNewsItem item : search(query)) {
                String key = item.link() == null || item.link().isBlank() ? item.title() : item.link();
                if (!key.isBlank()) {
                    uniqueItems.putIfAbsent(key, item);
                }
                if (uniqueItems.size() >= 20) {
                    return new ArrayList<>(uniqueItems.values());
                }
            }
        }
        return new ArrayList<>(uniqueItems.values());
    }

    private List<NaverNewsItem> search(String query) {
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

    private List<String> newsQueries(String industryName, OpenAiIndustryKeywordClient.IndustryNewsKeywords keywords) {
        Set<String> queries = new LinkedHashSet<>();
        List<String> primaryKeywords = keywords == null ? List.of() : keywords.primaryKeywords();
        List<String> trendKeywords = keywords == null ? List.of() : keywords.trendKeywords();
        List<String> busanKeywords = keywords == null ? List.of() : keywords.busanKeywords();

        if (primaryKeywords.isEmpty()) {
            primaryKeywords = fallbackPrimaryKeywords(industryName);
        }
        for (String keyword : primaryKeywords) {
            addQuery(queries, keyword + " 시장");
            addQuery(queries, keyword + " 업황");
            addQuery(queries, keyword + " 수출");
        }
        for (String trendKeyword : trendKeywords) {
            addQuery(queries, primaryKeywords.get(0) + " " + trendKeyword);
        }
        for (String busanKeyword : busanKeywords) {
            addQuery(queries, busanKeyword);
        }
        addQuery(queries, industryName + " 산업 동향");
        return queries.stream().limit(10).toList();
    }

    private List<String> fallbackPrimaryKeywords(String industryName) {
        String normalized = industryName
                .replace("제조업", "")
                .replace("및", " ")
                .replace("기타", " ")
                .replace(",", " ")
                .replaceAll("\\s+", " ")
                .trim();
        List<String> keywords = new ArrayList<>();
        if (!normalized.isBlank()) {
            keywords.add(normalized + " 제조업");
        }
        if (industryName.contains("기계") || industryName.contains("장비")) {
            keywords.add("기계장비 제조업");
            keywords.add("산업기계");
            keywords.add("자동화 설비");
            keywords.add("스마트팩토리 장비");
        }
        if (keywords.isEmpty()) {
            keywords.add(industryName);
        }
        return keywords.stream().distinct().limit(5).toList();
    }

    private void addQuery(Set<String> queries, String query) {
        String normalized = query == null ? "" : query.replaceAll("\\s+", " ").trim();
        if (!normalized.isBlank()) {
            queries.add(normalized);
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
