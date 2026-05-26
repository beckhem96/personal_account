package org.example.account.dto;

import java.math.BigDecimal;

public record LoanScheduleRow(
        int month,                  // 회차 (1부터 시작)
        BigDecimal payment,         // 해당 회차 납입액
        BigDecimal principal,       // 원금 상환분
        BigDecimal interest,        // 이자 납부분
        BigDecimal remainingBalance // 회차 종료 후 잔액
) {
}
