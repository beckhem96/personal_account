package org.example.account.dto;

import java.math.BigDecimal;
import java.util.Map;

public record NetWorthResponse(
        BigDecimal totalAssets,
        BigDecimal totalLiabilities, // 부채
        BigDecimal netWorth, // 순자산
        Map<String, BigDecimal> assetsByType
) {
}
