package org.example.account.dto;

import java.math.BigDecimal;

/**
 * 연말정산 정밀 계산 입력 — 결정세액·환급액까지 산출.
 *
 * <p>인적공제 인원은 본인 제외 부양가족 수(dependents)와 추가공제 대상 인원으로 받는다.
 * 금액은 모두 연간 합계.
 */
public record YearEndFullRequest(
        BigDecimal totalSalary,          // 총급여 (연봉, 비과세 제외)

        // 신용카드 등 소득공제 (기존 카드 계산 재사용)
        BigDecimal creditCardAmount,     // 신용카드
        BigDecimal debitCashAmount,      // 체크카드/현금영수증
        BigDecimal traditionalMarketAmount, // 전통시장
        BigDecimal publicTransportAmount,   // 대중교통

        // 인적공제 (본인 기본공제 1인은 항상 포함)
        Integer dependents,              // 본인 제외 기본공제 대상 부양가족 수
        Integer seniors,                 // 경로우대(70세 이상) 인원
        Integer disabled,                // 장애인 인원

        // 연금보험료 + 특별소득공제(보험료) — 4대보험
        BigDecimal nationalPension,      // 국민연금 납입액
        BigDecimal healthInsurance,      // 건강보험+장기요양 납입액
        BigDecimal employmentInsurance,  // 고용보험 납입액

        // 특별세액공제 + 연금계좌세액공제
        BigDecimal insurancePremium,     // 보장성보험료
        BigDecimal medicalExpense,       // 의료비
        BigDecimal educationExpense,     // 교육비
        BigDecimal donation,             // 기부금
        BigDecimal pensionSavings,       // 연금저축+IRP 납입액

        BigDecimal prepaidTax            // 기납부세액(원천징수 소득세 합계)
) {
}
