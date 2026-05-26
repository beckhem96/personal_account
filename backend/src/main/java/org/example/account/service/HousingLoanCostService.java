package org.example.account.service;

import org.example.account.dto.LoanCostRequest;
import org.example.account.dto.LoanCostResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 주택담보대출 실행 시 부대비용 계산.
 * - 근저당설정비: 채권최고액(대출액의 120%) × 0.24%
 * - 인지세: 대출액 구간별
 * - 감정평가수수료: 약 30만 (사용자 지정 시 포함, 미지정 시 0)
 */
@Service
public class HousingLoanCostService {

    private static final BigDecimal MAX_CLAIM_RATIO = new BigDecimal("1.2");      // 채권최고액 = 대출액 × 120%
    private static final BigDecimal MORTGAGE_RATE = new BigDecimal("0.0024");     // 0.24%
    private static final BigDecimal APPRAISAL_FEE_DEFAULT = new BigDecimal("300000");

    public LoanCostResponse calculate(LoanCostRequest request) {
        BigDecimal loan = nz(request.loanAmount());

        BigDecimal maxClaim = loan.multiply(MAX_CLAIM_RATIO);
        BigDecimal mortgage = maxClaim.multiply(MORTGAGE_RATE).setScale(0, RoundingMode.FLOOR);
        BigDecimal stamp = calculateLoanStampDuty(loan);
        BigDecimal appraisal = Boolean.TRUE.equals(request.includeAppraisalFee())
                ? APPRAISAL_FEE_DEFAULT
                : BigDecimal.ZERO;

        return LoanCostResponse.of(mortgage, stamp, appraisal);
    }

    /** 대출 약정서 인지세 — 대출액 구간별. */
    private BigDecimal calculateLoanStampDuty(BigDecimal loan) {
        if (loan.compareTo(new BigDecimal("50000000")) <= 0) return new BigDecimal("70000");
        if (loan.compareTo(new BigDecimal("100000000")) <= 0) return new BigDecimal("150000");
        if (loan.compareTo(new BigDecimal("1000000000")) <= 0) return new BigDecimal("350000");
        return new BigDecimal("350000");
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
