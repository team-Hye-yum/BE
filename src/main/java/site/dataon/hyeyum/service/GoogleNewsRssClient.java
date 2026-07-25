package site.dataon.hyeyum.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
