package org.example.account.dto;

import org.example.account.domain.AssetType;

import java.math.BigDecimal;

public record AssetRequest(
        AssetType type,
        String name,
        BigDecimal balance,
        BigDecimal purchasePrice
) {
}
