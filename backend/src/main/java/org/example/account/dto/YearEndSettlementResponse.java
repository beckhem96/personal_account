package org.example.account.dto;

import java.math.BigDecimal;

public record YearEndSettlementResponse(
        BigDecimal minUsageThreshold, // 최저 사용금액 (총급여 25%)
        BigDecimal creditDeduction, // 신용카드 공제액
        BigDecimal debitDeduction, // 체크카드/현금영수증 공제액
        BigDecimal marketDeduction, // 전통시장 공제액 (한도 적용 후)
        BigDecimal transportDeduction, // 대중교통 공제액 (한도 적용 후)
        BigDecimal generalLimit, // 일반 공제한도 (총급여 7천만원 기준 차등)
        BigDecimal totalDeduction, // 최종 합산 공제액
        String guideMessage // 가이드 메시지
) {
}
