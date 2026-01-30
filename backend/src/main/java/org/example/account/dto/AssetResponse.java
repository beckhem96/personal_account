package org.example.account.dto;

import org.example.account.domain.Asset;
import org.example.account.domain.AssetType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record AssetResponse(
        Long id,
        AssetType type,
        String name,
        BigDecimal balance,
        BigDecimal purchasePrice,
        Double returnRate // 수익률 (주식인 경우)
) {
    public static AssetResponse from(Asset asset) {
        Double rate = null;
        if (asset.getType() == AssetType.STOCK && asset.getPurchasePrice() != null && asset.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = asset.getBalance().subtract(asset.getPurchasePrice());
            // (Current - Purchase) / Purchase * 100
            rate = diff.divide(asset.getPurchasePrice(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();
        }

        return new AssetResponse(
                asset.getId(),
                asset.getType(),
                asset.getName(),
                asset.getBalance(),
                asset.getPurchasePrice(),
                rate
        );
    }
}
