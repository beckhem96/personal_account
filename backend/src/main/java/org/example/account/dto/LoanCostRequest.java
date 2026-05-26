package org.example.account.dto;

import java.math.BigDecimal;

public record LoanCostRequest(
        BigDecimal loanAmount,          // 대출 실행 금액
        Boolean includeAppraisalFee     // 감정평가수수료 포함 여부 (KB시세 있는 아파트는 보통 생략)
) {
}
