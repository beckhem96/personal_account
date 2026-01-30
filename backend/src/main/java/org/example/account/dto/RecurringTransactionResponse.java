package org.example.account.dto;

import org.example.account.domain.PaymentMethod;
import org.example.account.domain.RecurringTransaction;

import java.math.BigDecimal;

public record RecurringTransactionResponse(
        Long id,
        String name,
        BigDecimal amount,
        Integer dayOfMonth,
        PaymentMethod paymentMethod,
        String categoryName,
        String cardName
) {
    public static RecurringTransactionResponse from(RecurringTransaction rt) {
        return new RecurringTransactionResponse(
                rt.getId(),
                rt.getName(),
                rt.getAmount(),
                rt.getDayOfMonth(),
                rt.getPaymentMethod(),
                rt.getCategory().getName(),
                rt.getCard() != null ? rt.getCard().getName() : null
        );
    }
}
