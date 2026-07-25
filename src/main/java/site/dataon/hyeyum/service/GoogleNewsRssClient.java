package site.dataon.hyeyum.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
public class GoogleNewsRssClient {

    private static final List<String> OVERSEAS_COUNTRIES =
            List.of("미국", "중국", "일본", "독일", "EU", "싱가포르");

    private final OkHttpClient httpClient;
    private final int display;

    public GoogleNewsRssClient(@Value("${google.news.display:10}") int display) {
        this.httpClient =
                new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .callTimeout(30, TimeUnit.SECONDS)
                        .build();
        this.display = Math.max(1, Math.min(display, 20));
    }

    public List<NaverNewsSearchClient.NaverNewsItem> searchOverseasIndustryNews(String industryName) {
        String query = industryName + " (" + String.join(" OR ", OVERSEAS_COUNTRIES) + ") 산업 기술 정책 시장";
        return search(query);
    }

    public List<NaverNewsSearchClient.NaverNewsItem> searchIndustryEvidenceNews(String industryName) {
        return searchIndustryEvidenceNews(industryName, null);
    }

    public List<NaverNewsSearchClient.NaverNewsItem> searchIndustryEvidenceNews(
            String industryName, OpenAiIndustryKeywordClient.IndustryNewsKeywords keywords) {
        List<NaverNewsSearchClient.NaverNewsItem> items = new ArrayList<>();
        Set<String> seenLinks = new LinkedHashSet<>();
        for (String query : evidenceQueries(industryName, keywords)) {
            for (NaverNewsSearchClient.NaverNewsItem item : search(query)) {
                if (isUsableNewsLink(item.link()) && seenLinks.add(item.link())) {
                    items.add(item);
                }
                if (items.size() >= display) {
                    return items;
                }
            }
        }
        return items;
    }

    private List<NaverNewsSearchClient.NaverNewsItem> search(String query) {
        try {
            Request request =
                    new Request.Builder()
                            .url("https://news.google.com/rss/search?q="
                                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                                    + "&hl=ko&gl=KR&ceid=KR:ko")
                            .get()
                            .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return List.of();
                }
                return parse(response.body().bytes());
            }
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<NaverNewsSearchClient.NaverNewsItem> parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        NodeList nodes = document.getElementsByTagName("item");
        List<NaverNewsSearchClient.NaverNewsItem> items = new ArrayList<>();
        for (int index = 0; index < nodes.getLength() && items.size() < display; index++) {
            Element item = (Element) nodes.item(index);
            items.add(new NaverNewsSearchClient.NaverNewsItem(
                    text(item, "title"),
                    cleanGoogleDescription(text(item, "description")),
                    text(item, "link"),
                    text(item, "pubDate")));
        }
        return items;
    }

    private List<String> evidenceQueries(String industryName, OpenAiIndustryKeywordClient.IndustryNewsKeywords keywords) {
        Set<String> queries = new LinkedHashSet<>();
        List<String> primaryKeywords = keywords == null ? List.of() : keywords.primaryKeywords();
        List<String> trendKeywords = keywords == null ? List.of() : keywords.trendKeywords();
        List<String> busanKeywords = keywords == null ? List.of() : keywords.busanKeywords();
        if (primaryKeywords.isEmpty()) {
            primaryKeywords = List.of(industryName);
        }
        for (String keyword : primaryKeywords) {
            addQuery(queries, keyword + " 산업 동향");
            addQuery(queries, keyword + " 투자 수요");
        }
        for (String trendKeyword : trendKeywords) {
            addQuery(queries, primaryKeywords.get(0) + " " + trendKeyword);
        }
        for (String busanKeyword : busanKeywords) {
            addQuery(queries, busanKeyword + " 산업");
        }
        addQuery(queries, industryName + " 산업 동향 투자 수요 인력 친환경 디지털");
        return queries.stream().limit(8).toList();
    }

    private boolean isUsableNewsLink(String link) {
        String value = link == null ? "" : link.trim();
        return !value.isBlank()
                && !"https://news.google.com/".equals(value)
                && !"http://news.google.com/".equals(value);
    }

    private void addQuery(Set<String> queries, String query) {
        String normalized = query == null ? "" : query.replaceAll("\\s+", " ").trim();
        if (!normalized.isBlank()) {
            queries.add(normalized);
        }
    }

    private String text(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    private String cleanGoogleDescription(String value) throws IOException {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("<[^>]+>", " ")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
