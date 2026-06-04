package org.example.account.dto;

import java.math.BigDecimal;

/** 연말정산 정밀 계산 결과 — 단계별 금액 + 결정세액 + 환급/추가납부. */
public record YearEndFullResponse(
        // 1단계: 근로소득금액
        BigDecimal grossSalary,
        BigDecimal earnedIncomeDeduction,   // 근로소득공제
        BigDecimal earnedIncome,            // 근로소득금액

        // 2단계: 종합소득공제 → 과세표준
        BigDecimal personalDeduction,       // 인적공제
        BigDecimal pensionInsuranceDeduction, // 연금보험료공제(국민연금)
        BigDecimal specialIncomeDeduction,  // 특별소득공제(건강/고용보험)
        BigDecimal cardDeduction,           // 신용카드 등 소득공제
        BigDecimal totalIncomeDeduction,    // 종합소득공제 합계
        BigDecimal taxBase,                 // 과세표준

        // 3단계: 산출세액
        BigDecimal calculatedTax,

        // 4단계: 세액공제 → 결정세액
        BigDecimal earnedIncomeTaxCredit,   // 근로소득세액공제
        BigDecimal insuranceCredit,         // 보험료 세액공제
        BigDecimal medicalCredit,           // 의료비 세액공제
        BigDecimal educationCredit,         // 교육비 세액공제
        BigDecimal donationCredit,          // 기부금 세액공제
        BigDecimal pensionAccountCredit,    // 연금계좌 세액공제
        BigDecimal totalTaxCredit,          // 세액공제 합계
        BigDecimal determinedTax,           // 결정세액(소득세)
        BigDecimal localIncomeTax,          // 지방소득세(결정세액 10%)

        // 5단계: 정산
        BigDecimal prepaidTax,              // 기납부세액
        BigDecimal refundOrPay,             // +환급 / -추가납부 (소득세 기준)

        String guideMessage
) {
}
