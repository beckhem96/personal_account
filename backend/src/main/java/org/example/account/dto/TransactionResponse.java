package org.example.account.dto;

import org.example.account.domain.PaymentMethod;
import org.example.account.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        LocalDate date,
        BigDecimal amount,
        String memo,
        PaymentMethod paymentMethod,
        String categoryName,
        Long categoryId, // Added for edit form
        boolean isConfirmed,
        String cardName,
        Long cardId // Added for edit form
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDate(),
                transaction.getAmount(),
                transaction.getMemo(),
                transaction.getPaymentMethod(),
                transaction.getCategory().getName(),
                transaction.getCategory().getId(),
                transaction.isConfirmed(),
                transaction.getCard() != null ? transaction.getCard().getName() : null,
                transaction.getCard() != null ? transaction.getCard().getId() : null
        );
    }
}
