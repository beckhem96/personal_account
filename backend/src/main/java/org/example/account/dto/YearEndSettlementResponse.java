package org.example.account.dto;

import java.math.BigDecimal;

public record YearEndSettlementResponse(
        BigDecimal minUsageThreshold, // 최저 사용금액 (총급여 25%)
        BigDecimal estimatedDeduction, // 예상 소득공제액
        String guideMessage // 가이드 메시지
) {
}
