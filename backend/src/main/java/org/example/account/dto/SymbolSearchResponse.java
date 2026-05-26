package org.example.account.dto;

public record SymbolSearchResponse(
        String symbol,
        String name,
        String type,
        String region,
        String currency
) {
}
