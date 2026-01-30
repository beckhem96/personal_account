package org.example.account.dto;

import java.math.BigDecimal;

public record BudgetRequest(Integer year, Integer month, Long categoryId, BigDecimal amount) {
}
