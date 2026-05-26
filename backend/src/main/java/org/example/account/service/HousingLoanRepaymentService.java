package org.example.account.service;

import org.example.account.domain.RepaymentType;
import org.example.account.dto.LoanRepaymentRequest;
import org.example.account.dto.LoanRepaymentResponse;
import org.example.account.dto.LoanScheduleRow;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 대출 상환 스케줄 계산.
 * 지원: 원리금균등(PRINCIPAL_INTEREST), 원금균등(PRINCIPAL_ONLY), 만기일시(BULLET) + 거치기간.
 * 모든 금액은 원 단위 정수(FLOOR 반올림)로 출력. 마지막 회차에서 잔액을 0으로 정확히 맞춤.
 */
@Service
public class HousingLoanRepaymentService {

    private static final MathContext MC = MathContext.DECIMAL128;

    public LoanRepaymentResponse calculate(LoanRepaymentRequest request) {
        BigDecimal principal = nz(request.principal());
        BigDecimal annualRate = nz(request.annualRatePercent());
        int totalMonths = request.termMonths() == null ? 0 : request.termMonths();
        int grace = request.gracePeriodMonths() == null ? 0 : request.gracePeriodMonths();
        RepaymentType type = request.repaymentType() == null ? RepaymentType.PRINCIPAL_INTEREST : request.repaymentType();

        if (totalMonths <= 0 || principal.signum() <= 0) {
            return new LoanRepaymentResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
        }
        if (grace >= totalMonths) grace = totalMonths - 1;

        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("100"), MC).divide(new BigDecimal("12"), MC);

        List<LoanScheduleRow> schedule = switch (type) {
            case PRINCIPAL_INTEREST -> buildEqualPayment(principal, monthlyRate, totalMonths, grace);
            case PRINCIPAL_ONLY -> buildEqualPrincipal(principal, monthlyRate, totalMonths, grace);
            case BULLET -> buildBullet(principal, monthlyRate, totalMonths);
        };

        BigDecimal totalInterest = schedule.stream()
                .map(LoanScheduleRow::interest)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPayment = schedule.stream()
                .map(LoanScheduleRow::payment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal first = schedule.get(0).payment();
        BigDecimal last = schedule.get(schedule.size() - 1).payment();

        return new LoanRepaymentResponse(first, last, totalInterest, totalPayment, schedule);
    }

    // 원리금균등: 거치기간 동안 이자만, 거치 종료 후 잔여 개월수로 PMT 재계산
    private List<LoanScheduleRow> buildEqualPayment(BigDecimal principal, BigDecimal monthlyRate,
                                                     int totalMonths, int grace) {
        List<LoanScheduleRow> rows = new ArrayList<>(totalMonths);
        BigDecimal balance = principal;

        for (int m = 1; m <= grace; m++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(0, RoundingMode.FLOOR);
            rows.add(new LoanScheduleRow(m, interest, BigDecimal.ZERO, interest, balance));
        }

        int repayMonths = totalMonths - grace;
        BigDecimal pmt = computePmt(balance, monthlyRate, repayMonths);

        for (int m = grace + 1; m <= totalMonths; m++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(0, RoundingMode.FLOOR);
            BigDecimal payment;
            BigDecimal principalPart;
            if (m == totalMonths) {
                // 마지막 회차: 잔액 + 이자
                principalPart = balance;
                payment = balance.add(interest);
                balance = BigDecimal.ZERO;
            } else {
                payment = pmt;
                principalPart = payment.subtract(interest);
                balance = balance.subtract(principalPart);
            }
            rows.add(new LoanScheduleRow(m, payment, principalPart, interest, balance));
        }
        return rows;
    }

    // 원금균등: 거치 동안 이자만, 거치 종료 후 (잔여 개월) 균등 원금
    private List<LoanScheduleRow> buildEqualPrincipal(BigDecimal principal, BigDecimal monthlyRate,
                                                       int totalMonths, int grace) {
        List<LoanScheduleRow> rows = new ArrayList<>(totalMonths);
        BigDecimal balance = principal;

        for (int m = 1; m <= grace; m++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(0, RoundingMode.FLOOR);
            rows.add(new LoanScheduleRow(m, interest, BigDecimal.ZERO, interest, balance));
        }

        int repayMonths = totalMonths - grace;
        BigDecimal monthlyPrincipal = balance.divide(new BigDecimal(repayMonths), 0, RoundingMode.FLOOR);

        for (int m = grace + 1; m <= totalMonths; m++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(0, RoundingMode.FLOOR);
            BigDecimal principalPart;
            if (m == totalMonths) {
                principalPart = balance;
                balance = BigDecimal.ZERO;
            } else {
                principalPart = monthlyPrincipal;
                balance = balance.subtract(principalPart);
            }
            BigDecimal payment = principalPart.add(interest);
            rows.add(new LoanScheduleRow(m, payment, principalPart, interest, balance));
        }
        return rows;
    }

    // 만기일시: 매월 이자만, 마지막 회차 원금 일시 + 이자
    private List<LoanScheduleRow> buildBullet(BigDecimal principal, BigDecimal monthlyRate, int totalMonths) {
        List<LoanScheduleRow> rows = new ArrayList<>(totalMonths);
        BigDecimal interestMonthly = principal.multiply(monthlyRate).setScale(0, RoundingMode.FLOOR);
        for (int m = 1; m < totalMonths; m++) {
            rows.add(new LoanScheduleRow(m, interestMonthly, BigDecimal.ZERO, interestMonthly, principal));
        }
        rows.add(new LoanScheduleRow(totalMonths, principal.add(interestMonthly), principal, interestMonthly, BigDecimal.ZERO));
        return rows;
    }

    /**
     * 원리금균등 월 납입액 PMT = P * r * (1+r)^n / ((1+r)^n - 1).
     * r=0이면 단순 P/n.
     */
    private BigDecimal computePmt(BigDecimal principal, BigDecimal monthlyRate, int months) {
        if (monthlyRate.signum() == 0) {
            return principal.divide(new BigDecimal(months), 0, RoundingMode.FLOOR);
        }
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal pow = onePlusR.pow(months, MC);
        BigDecimal numerator = principal.multiply(monthlyRate, MC).multiply(pow, MC);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE, MC);
        return numerator.divide(denominator, MC).setScale(0, RoundingMode.FLOOR);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
