package org.example.account.dto;

import org.example.account.domain.MyStock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record MyStockResponse(
        Long id,
        String ticker,
        String companyName,
        BigDecimal purchasePrice,
        Integer quantity,
        BigDecimal currentPrice,
        BigDecimal valuation,
        Double returnRate,
        LocalDateTime lastSyncedAt
) {
    public static MyStockResponse from(MyStock stock) {
        BigDecimal valuation = null;
        Double returnRate = null;

        if (stock.getCurrentPrice() != null) {
            valuation = stock.getCurrentPrice().multiply(BigDecimal.valueOf(stock.getQuantity()));

            if (stock.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal diff = stock.getCurrentPrice().subtract(stock.getPurchasePrice());
                returnRate = diff.divide(stock.getPurchasePrice(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .doubleValue();
            }
        }

        return new MyStockResponse(
                stock.getId(),
                stock.getTicker(),
                stock.getCompanyName(),
                stock.getPurchasePrice(),
                stock.getQuantity(),
                stock.getCurrentPrice(),
                valuation,
                returnRate,
                stock.getLastSyncedAt()
        );
    }
}
