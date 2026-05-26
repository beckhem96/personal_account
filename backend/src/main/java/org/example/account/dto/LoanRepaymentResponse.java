package org.example.account.dto;

import java.math.BigDecimal;
import java.util.List;

public record LoanRepaymentResponse(
        BigDecimal firstMonthPayment, // 첫 회차 납입액 (대표값)
        BigDecimal lastMonthPayment,  // 마지막 회차 납입액 (만기일시는 매우 큼)
        BigDecimal totalInterest,     // 총 이자
        BigDecimal totalPayment,      // 총 납입 (원금 + 이자)
        List<LoanScheduleRow> schedule
) {
}
