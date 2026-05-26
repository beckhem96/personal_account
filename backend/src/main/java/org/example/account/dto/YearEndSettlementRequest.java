package org.example.account.dto;

import java.math.BigDecimal;

public record YearEndSettlementRequest(
        BigDecimal totalSalary, // 총급여
        BigDecimal creditCardAmount, // 신용카드 사용액
        BigDecimal debitCashAmount, // 체크카드/현금영수증 사용액
        BigDecimal traditionalMarketAmount, // 전통시장 사용액
        BigDecimal publicTransportAmount // 대중교통 사용액
) {
}
