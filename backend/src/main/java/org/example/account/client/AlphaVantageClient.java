package org.example.account.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.example.account.dto.SymbolSearchResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AlphaVantageClient {

    private final RestClient restClient;
    private final String apiKey;

    public AlphaVantageClient(
            @Qualifier("alphaVantageRestClient") RestClient restClient,
            @Value("${alphavantage.api-key}") String apiKey
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    /**
     * 티커/회사명 검색 — SYMBOL_SEARCH
     */
    public List<SymbolSearchResponse> symbolSearch(String keywords) {
        JsonNode root = fetchJson("function", "SYMBOL_SEARCH", "keywords", keywords);

        List<SymbolSearchResponse> results = new ArrayList<>();
        if (root != null && root.has("bestMatches")) {
            for (JsonNode match : root.get("bestMatches")) {
                results.add(new SymbolSearchResponse(
                        match.get("1. symbol").asText(),
                        match.get("2. name").asText(),
                        match.get("3. type").asText(),
                        match.get("4. region").asText(),
                        match.get("8. currency").asText()
                ));
            }
        }
        return results;
    }

    /**
     * 현재가 조회 — GLOBAL_QUOTE
     */
    public BigDecimal getGlobalQuote(String symbol) {
        JsonNode root = fetchJson("function", "GLOBAL_QUOTE", "symbol", symbol);

        if (root != null && root.has("Global Quote")) {
            JsonNode quote = root.get("Global Quote");
            String price = quote.get("05. price").asText();
            return new BigDecimal(price);
        }
        throw new IllegalStateException("Alpha Vantage 현재가 조회 실패: " + symbol);
    }

    /**
     * RSI(14) 조회
     */
    public Map<String, String> getRsi(String symbol) {
        JsonNode root = fetchJson("function", "RSI", "symbol", symbol,
                "interval", "daily", "time_period", "14", "series_type", "close");
        return extractLatestIndicator(root, "Technical Analysis: RSI");
    }

    /**
     * SMA 조회
     */
    public Map<String, String> getSma(String symbol, int timePeriod) {
        JsonNode root = fetchJson("function", "SMA", "symbol", symbol,
                "interval", "daily", "time_period", String.valueOf(timePeriod), "series_type", "close");
        return extractLatestIndicator(root, "Technical Analysis: SMA");
    }

    /**
     * Bollinger Bands(20, 2) 조회
     */
    public Map<String, String> getBbands(String symbol) {
        JsonNode root = fetchJson("function", "BBANDS", "symbol", symbol,
                "interval", "daily", "time_period", "20", "series_type", "close");
        return extractLatestIndicator(root, "Technical Analysis: BBANDS");
    }

    /**
     * 종목 뉴스/센티먼트 조회
     */
    public List<Map<String, String>> getNewsSentiment(String ticker, int limit) {
        JsonNode root = fetchJson("function", "NEWS_SENTIMENT", "tickers", ticker, "limit", String.valueOf(limit));

        List<Map<String, String>> newsList = new ArrayList<>();
        if (root != null && root.has("feed")) {
            for (JsonNode article : root.get("feed")) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("title", article.path("title").asText(""));
                item.put("url", article.path("url").asText(""));
                item.put("summary", article.path("summary").asText(""));
                item.put("overall_sentiment_label", article.path("overall_sentiment_label").asText(""));
                newsList.add(item);
            }
        }
        return newsList;
    }

    /**
     * 공통 API 호출 — query param을 key-value 쌍으로 받아 URI 빌드
     */
    private JsonNode fetchJson(String... params) {
        try {
            return restClient.get()
                    .uri(uri -> {
                        uri.queryParam("datatype", "json");
                        uri.queryParam("apikey", apiKey);
                        for (int i = 0; i < params.length; i += 2) {
                            uri.queryParam(params[i], params[i + 1]);
                        }
                        return uri.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            throw new IllegalStateException("Alpha Vantage API 호출 실패 (HTTP " + status + "): " + e.getMessage());
        }
    }

    /**
     * Alpha Vantage 기술적 지표 응답 파싱 — 날짜 키 기반 오브젝트에서 최신 엔트리 추출
     */
    private Map<String, String> extractLatestIndicator(JsonNode root, String analysisKey) {
        Map<String, String> result = new LinkedHashMap<>();
        if (root != null && root.has(analysisKey)) {
            JsonNode analysis = root.get(analysisKey);
            var fields = analysis.fields();
            if (fields.hasNext()) {
                var entry = fields.next();
                result.put("date", entry.getKey());
                JsonNode values = entry.getValue();
                values.fields().forEachRemaining(f -> result.put(f.getKey(), f.getValue().asText()));
            }
        }
        return result;
    }
}
