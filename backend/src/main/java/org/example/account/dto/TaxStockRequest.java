package org.example.account.dto;

import java.math.BigDecimal;

public record TaxStockRequest(BigDecimal totalSellAmount, BigDecimal totalBuyAmount) {
}
