package org.example.account.dto;

import java.math.BigDecimal;

public record MyStockRequest(
        String ticker,
        String companyName,
        BigDecimal purchasePrice,
        Integer quantity
) {
}
