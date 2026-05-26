package org.example.account.dto;

import org.example.account.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionRequest(
        String name,
        BigDecimal amount,
        Integer dayOfMonth,
        PaymentMethod paymentMethod,
        Long categoryId,
        Long cardId,
        Long assetId,
        Long toAssetId,
        LocalDate startDate,
        LocalDate endDate
) {
}
