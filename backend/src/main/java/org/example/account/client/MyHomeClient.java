package org.example.account.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class MyHomeClient {

    private final XmlMapper xmlMapper = new XmlMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;
    private final String apiKey;
    private final String baseUrl;
    private final String rentalPath;
    private final String salesPath;

    public MyHomeClient(
            @Qualifier("myHomeRestClient") RestClient restClient,
            @Value("${myhome.api-key}") String apiKey,
            @Value("${myhome.base-url}") String baseUrl,
            @Value("${myhome.rental-path}") String rentalPath,
            @Value("${myhome.sales-path}") String salesPath
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.rentalPath = rentalPath;
        this.salesPath = salesPath;
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<JsonNode> fetchAll() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        List<JsonNode> all = new ArrayList<>();
        all.addAll(fetchOne(rentalPath));
        all.addAll(fetchOne(salesPath));
        return all;
    }

    private List<JsonNode> fetchOne(String path) {
        String query = "serviceKey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&PG_SZ=100"
                + "&PAGE=1";
        URI uri = URI.create(baseUrl + path + "?" + query);

        try {
            String xml = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (xml == null || xml.isBlank()) {
                return Collections.emptyList();
            }

            JsonNode root;
            String trimmed = xml.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                root = objectMapper.readTree(xml.getBytes(StandardCharsets.UTF_8));
            } else {
                root = xmlMapper.readTree(xml.getBytes(StandardCharsets.UTF_8));
            }

            JsonNode items = root.path("response").path("body").path("item");
            if (items.isMissingNode() || items.isNull()) {
                items = root.path("hsprRcritList").path("hsprRcrit");
            }
            if (items.isMissingNode() || items.isNull()) {
                items = root.path("body").path("items").path("item");
            }
            if (items.isMissingNode() || items.isNull()) {
                return Collections.emptyList();
            }

            List<JsonNode> list = new ArrayList<>();
            if (items.isArray()) {
                items.forEach(list::add);
            } else if (items.isObject()) {
                list.add(items);
            }
            return list;
        } catch (Exception e) {
            log.warn("마이홈 API 호출 실패 path={} : {}", path, e.getMessage());
            return Collections.emptyList();
        }
    }
}
