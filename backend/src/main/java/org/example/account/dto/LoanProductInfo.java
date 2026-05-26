package org.example.account.dto;

import org.example.account.domain.LoanProductCode;

import java.math.BigDecimal;
import java.util.List;

public record LoanProductInfo(
        LoanProductCode code,
        String name,                // 표시 이름
        String description,         // 한 줄 설명
        BigDecimal maxLoanAmount,   // 최대 대출 한도 (참고치)
        BigDecimal referenceRate,   // 참고 금리 (%, 시장 평균)
        List<String> eligibility    // 이용 조건 안내 문구
) {
}
