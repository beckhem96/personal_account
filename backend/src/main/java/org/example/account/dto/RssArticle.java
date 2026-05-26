package org.example.account.dto;

public record RssArticle(
        String title,
        String description,
        String link,
        String pubDate,
        String category
) {
}
