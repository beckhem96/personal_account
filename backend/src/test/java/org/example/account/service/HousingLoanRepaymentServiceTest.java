package org.example.account.service;

import org.example.account.domain.LoanProductCode;
import org.example.account.domain.RepaymentType;
import org.example.account.dto.LoanRepaymentRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HousingLoanRepaymentServiceTest {

    private final HousingLoanRepaymentService service = new HousingLoanRepaymentService();

    @Test
    void 원리금균등_3억_4퍼센트_30년() {
        // PMT 표준식 결과: ~143만원/월
        var r = service.calculate(new LoanRepaymentRequest(
                new BigDecimal("300000000"), new BigDecimal("4.0"), 360,
                RepaymentType.PRINCIPAL_INTEREST, 0, LoanProductCode.GENERAL));

        assertThat(r.schedule()).hasSize(360);
        assertThat(r.firstMonthPayment()).isBetween(new BigDecimal("1430000"), new BigDecimal("1435000"));
        // 마지막 회차 잔액은 0
        assertThat(r.schedule().get(359).remainingBalance()).isEqualByComparingTo("0");
        // 총 이자가 합리적인 범위인지 확인 (~2.15억)
        assertThat(r.totalInterest()).isBetween(new BigDecimal("210000000"), new BigDecimal("220000000"));
    }

    @Test
    void 원금균등_2억_5퍼센트_20년() {
        // 월 원금 = 2억/240 = 833,333. 첫 회차 이자 = 2억×5%/12 = 833,333. 첫 회차 = 약 1,666,666
        var r = service.calculate(new LoanRepaymentRequest(
                new BigDecimal("200000000"), new BigDecimal("5.0"), 240,
                RepaymentType.PRINCIPAL_ONLY, 0, LoanProductCode.GENERAL));

        assertThat(r.schedule()).hasSize(240);
        assertThat(r.firstMonthPayment()).isBetween(new BigDecimal("1660000"), new BigDecimal("1670000"));
        // 마지막 회차 납입액은 첫 회차보다 작아야 (이자 감소)
        assertThat(r.lastMonthPayment()).isLessThan(r.firstMonthPayment());
        assertThat(r.schedule().get(239).remainingBalance()).isEqualByComparingTo("0");
    }

    @Test
    void 만기일시_1억_3퍼센트_5년() {
        // 매월 이자 = 1억×3%/12 = 250,000. 마지막 회차 = 1억 + 25만
        var r = service.calculate(new LoanRepaymentRequest(
                new BigDecimal("100000000"), new BigDecimal("3.0"), 60,
                RepaymentType.BULLET, 0, LoanProductCode.GENERAL));

        assertThat(r.firstMonthPayment()).isEqualByComparingTo("250000");
        assertThat(r.lastMonthPayment()).isEqualByComparingTo("100250000");
        assertThat(r.totalInterest()).isEqualByComparingTo("15000000"); // 25만 × 60
    }

    @Test
    void 원리금균등_거치기간_5년_30년_4퍼센트() {
        // 거치 60개월 동안 매월 이자만, 61개월차부터 240개월 PMT 재계산
        var r = service.calculate(new LoanRepaymentRequest(
                new BigDecimal("300000000"), new BigDecimal("4.0"), 360,
                RepaymentType.PRINCIPAL_INTEREST, 60, LoanProductCode.GENERAL));

        assertThat(r.schedule()).hasSize(360);
        // 1~60회차는 이자만 — 원금 상환 0
        assertThat(r.schedule().get(0).principal()).isEqualByComparingTo("0");
        assertThat(r.schedule().get(59).principal()).isEqualByComparingTo("0");
        // 61회차부터 원금 상환 시작
        assertThat(r.schedule().get(60).principal()).isGreaterThan(BigDecimal.ZERO);
        // 마지막 회차 잔액 0
        assertThat(r.schedule().get(359).remainingBalance()).isEqualByComparingTo("0");
    }

    @Test
    void 금리_0퍼센트_원리금균등() {
        // 무이자: 월납 = 원금/개월수
        var r = service.calculate(new LoanRepaymentRequest(
                new BigDecimal("120000000"), BigDecimal.ZERO, 120,
                RepaymentType.PRINCIPAL_INTEREST, 0, null));
        assertThat(r.firstMonthPayment()).isEqualByComparingTo("1000000");
        assertThat(r.totalInterest()).isEqualByComparingTo("0");
    }
}
