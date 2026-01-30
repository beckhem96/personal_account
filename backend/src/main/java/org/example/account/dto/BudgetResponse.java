package org.example.account.dto;

import org.example.account.domain.Budget;

import java.math.BigDecimal;

public record BudgetResponse(Long id, Integer year, Integer month, BigDecimal amount, String categoryName) {
    public static BudgetResponse from(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getYear(),
                budget.getMonth(),
                budget.getAmount(),
                budget.getCategory().getName()
        );
    }
}
