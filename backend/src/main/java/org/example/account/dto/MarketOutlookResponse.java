package org.example.account.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MarketOutlookResponse(
        String report,
        LocalDateTime generatedAt,
        List<RssArticle> sources
) {
}
