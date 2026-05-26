package org.example.account.dto;

import org.example.account.domain.PaymentMethod;
import org.example.account.domain.RecurringTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionResponse(
        Long id,
        String name,
        BigDecimal amount,
        Integer dayOfMonth,
        PaymentMethod paymentMethod,
        String categoryName,
        Long categoryId,
        String cardName,
        Long cardId,
        Long assetId,
        String assetName,
        Long toAssetId,
        String toAssetName,
        LocalDate startDate,
        LocalDate endDate
) {
    public static RecurringTransactionResponse from(RecurringTransaction rt) {
        return new RecurringTransactionResponse(
                rt.getId(),
                rt.getName(),
                rt.getAmount(),
                rt.getDayOfMonth(),
                rt.getPaymentMethod(),
                rt.getCategory().getName(),
                rt.getCategory().getId(),
                rt.getCard() != null ? rt.getCard().getName() : null,
                rt.getCard() != null ? rt.getCard().getId() : null,
                rt.getAsset() != null ? rt.getAsset().getId() : null,
                rt.getAsset() != null ? rt.getAsset().getName() : null,
                rt.getToAsset() != null ? rt.getToAsset().getId() : null,
                rt.getToAsset() != null ? rt.getToAsset().getName() : null,
                rt.getStartDate(),
                rt.getEndDate()
        );
    }
}
