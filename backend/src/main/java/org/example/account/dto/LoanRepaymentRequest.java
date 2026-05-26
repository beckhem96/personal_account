package org.example.account.dto;

import org.example.account.domain.LoanProductCode;
import org.example.account.domain.RepaymentType;

import java.math.BigDecimal;

public record LoanRepaymentRequest(
        BigDecimal principal,           // 대출 원금
        BigDecimal annualRatePercent,   // 연 금리 (%, 예: 4.5)
        Integer termMonths,             // 총 대출 기간 (개월)
        RepaymentType repaymentType,    // 상환방식
        Integer gracePeriodMonths,      // 거치기간 (개월, 0이면 거치 없음)
        LoanProductCode productCode     // 상품군 (안내용, optional)
) {
}
