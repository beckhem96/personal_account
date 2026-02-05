package org.example.account.dto;

import org.example.account.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        LocalDate date,
        BigDecimal amount,
        String memo,
        PaymentMethod paymentMethod,
        Long categoryId,
        Boolean isConfirmed,
        Long cardId,
        Long assetId,
        Long toAssetId // TRANSFER용
) {
}