package org.example.account.dto;

import java.util.List;
import java.util.Map;

public record StockAnalysisResponse(
        String ticker,
        String report,
        Map<String, String> indicators,
        List<NewsItem> news
) {
    public record NewsItem(
            String title,
            String url,
            String summary,
            String sentiment
    ) {
    }
}
