package org.example.account.dto;

import java.math.BigDecimal;

public record TaxStockResponse(
        BigDecimal profit,          // 양도차익
        BigDecimal deduction,       // 기본공제 (250만원)
        BigDecimal taxBase,         // 과세표준
        BigDecimal estimatedTax     // 예상 세액
) {
}
