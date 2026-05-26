package org.example.account.client;

import lombok.extern.slf4j.Slf4j;
import org.example.account.dto.RssArticle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CnbcRssClient {

    private final HttpClient httpClient;
    private final Map<String, String> feedUrls;

    private static final int MAX_ARTICLES_PER_FEED = 60;

    public CnbcRssClient(
            @Value("${cnbc.rss.address-pre}") String addressPre,
            @Value("${cnbc.rss.address-post}") String addressPost,
            @Value("${cnbc.rss.top-news}") String topNews,
            @Value("${cnbc.rss.world-news}") String worldNews,
            @Value("${cnbc.rss.finance}") String finance,
            @Value("${cnbc.rss.technology}") String technology
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.feedUrls = new LinkedHashMap<>();
        feedUrls.put("Top News", addressPre + topNews + addressPost);
        feedUrls.put("World News", addressPre + worldNews + addressPost);
        feedUrls.put("Finance", addressPre + finance + addressPost);
        feedUrls.put("Technology", addressPre + technology + addressPost);
    }

    public List<RssArticle> fetchAllFeeds() {
        List<RssArticle> allArticles = new ArrayList<>();

        for (Map.Entry<String, String> entry : feedUrls.entrySet()) {
            try {
                List<RssArticle> articles = fetchFeed(entry.getValue(), entry.getKey());
                allArticles.addAll(articles);
            } catch (Exception e) {
                log.warn("RSS 피드 수집 실패 [{}]: {}", entry.getKey(), e.getMessage());
            }
        }

        log.info("CNBC RSS 총 {}건 수집 완료", allArticles.size());
        return allArticles;
    }

    private List<RssArticle> fetchFeed(String url, String category) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (compatible; AccountApp/1.0)")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode());
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(response.body());

        NodeList items = doc.getElementsByTagName("item");
        List<RssArticle> articles = new ArrayList<>();

        int limit = Math.min(items.getLength(), MAX_ARTICLES_PER_FEED);
        for (int i = 0; i < limit; i++) {
            Element item = (Element) items.item(i);
            String title = getTextContent(item, "title");
            String description = getTextContent(item, "description");
            String link = getTextContent(item, "link");
            String pubDate = getTextContent(item, "pubDate");

            if (title != null && !title.isBlank()) {
                articles.add(new RssArticle(title.trim(), description != null ? description.trim() : "", link != null ? link.trim() : "", pubDate != null ? pubDate.trim() : "", category));
            }
        }

        return articles;
    }

    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }
}
