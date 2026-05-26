package org.example.account.service;

import lombok.RequiredArgsConstructor;
import org.example.account.dto.AcquisitionCostRequest;
import org.example.account.dto.AcquisitionCostResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 주택 구매 부대비용 종합 계산.
 * - 취득세 + 지방교육세 + 농특세: HousingTaxService 위임
 * - 중개수수료/법무사/인지세/국민주택채권 할인손실: 본 클래스에서 계산
 */
@Service
@RequiredArgsConstructor
public class HousingAcquisitionCostService {

    private final HousingTaxService taxService;

    // 중개수수료 상한요율 구간 (매매)
    private static final BigDecimal P_5천 = new BigDecimal("50000000");
    private static final BigDecimal P_2억 = new BigDecimal("200000000");
    private static final BigDecimal P_9억 = new BigDecimal("900000000");
    private static final BigDecimal P_12억 = new BigDecimal("1200000000");
    private static final BigDecimal P_15억 = new BigDecimal("1500000000");

    // 채권 매입율 구간 (시가표준액 기준)
    private static final BigDecimal SMP_2천 = new BigDecimal("20000000");
    private static final BigDecimal SMP_5천 = new BigDecimal("50000000");
    private static final BigDecimal SMP_1억 = new BigDecimal("100000000");
    private static final BigDecimal SMP_1억6 = new BigDecimal("160000000");
    private static final BigDecimal SMP_2억6 = new BigDecimal("260000000");
    private static final BigDecimal SMP_6억 = new BigDecimal("600000000");

    private static final BigDecimal DEFAULT_BOND_DISCOUNT_RATE = new BigDecimal("8"); // % — 미입력 시 기본
    private static final BigDecimal DEFAULT_SMP_RATIO = new BigDecimal("0.7");        // 시가표준액 추정 비율

    public AcquisitionCostResponse calculate(AcquisitionCostRequest request) {
        BigDecimal price = nz(request.salePrice());
        boolean regulated = Boolean.TRUE.equals(request.isRegulatedArea());
        boolean firstTime = Boolean.TRUE.equals(request.isFirstTime());

        var tax = taxService.calculate(price, request.houseCount(), regulated, firstTime, nz(request.exclusiveAreaSqm()));

        BigDecimal brokerFee = calculateBrokerFee(price);
        BigDecimal stampDuty = calculateStampDuty(price);
        BigDecimal judicialFee = calculateJudicialFee(price);
        BigDecimal bondLoss = calculateBondLoss(price, request.standardMarketPrice(), request.bondDiscountRate());

        return AcquisitionCostResponse.of(
                tax.acquisitionTax(),
                tax.localEducationTax(),
                tax.ruralSpecialTax(),
                brokerFee,
                judicialFee,
                stampDuty,
                bondLoss
        );
    }

    /** 중개수수료 상한요율 (협의 가능). 1억 미만 정액 한도 반영. */
    private BigDecimal calculateBrokerFee(BigDecimal price) {
        BigDecimal rate;
        if (price.compareTo(P_5천) < 0) {
            BigDecimal fee = price.multiply(new BigDecimal("0.006"));
            return fee.min(new BigDecimal("250000")).setScale(0, RoundingMode.FLOOR);
        } else if (price.compareTo(P_2억) < 0) {
            BigDecimal fee = price.multiply(new BigDecimal("0.005"));
            return fee.min(new BigDecimal("800000")).setScale(0, RoundingMode.FLOOR);
        } else if (price.compareTo(P_9억) < 0) {
            rate = new BigDecimal("0.004");
        } else if (price.compareTo(P_12억) < 0) {
            rate = new BigDecimal("0.005");
        } else if (price.compareTo(P_15억) < 0) {
            rate = new BigDecimal("0.006");
        } else {
            rate = new BigDecimal("0.007");
        }
        return price.multiply(rate).setScale(0, RoundingMode.FLOOR);
    }

    /** 인지세: 계약서 금액 기준 (단순화 — 매매 표준). */
    private BigDecimal calculateStampDuty(BigDecimal price) {
        if (price.compareTo(new BigDecimal("100000000")) < 0) return new BigDecimal("70000");
        if (price.compareTo(new BigDecimal("1000000000")) < 0) return new BigDecimal("150000");
        if (price.compareTo(new BigDecimal("3000000000")) < 0) return new BigDecimal("350000");
        return new BigDecimal("350000");
    }

    /** 법무사 보수 (등기 대행). 시장 평균값을 단순 선형 근사. */
    private BigDecimal calculateJudicialFee(BigDecimal price) {
        BigDecimal raw = price.multiply(new BigDecimal("0.0007"));
        BigDecimal floor = new BigDecimal("300000");
        BigDecimal ceil = new BigDecimal("1500000");
        return raw.max(floor).min(ceil).setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 국민주택채권 매입 → 즉시 매각 시 할인손실.
     * 시가표준액 미입력 시 매매가의 70%로 추정. 할인율(%) 미입력 시 기본 8%.
     */
    private BigDecimal calculateBondLoss(BigDecimal price, BigDecimal standardMarketPrice, BigDecimal discountRatePercent) {
        BigDecimal smp = standardMarketPrice;
        if (smp == null || smp.signum() == 0) {
            smp = price.multiply(DEFAULT_SMP_RATIO).setScale(0, RoundingMode.FLOOR);
        }
        BigDecimal purchaseRate = resolveBondPurchaseRate(smp);
        BigDecimal purchaseAmount = smp.multiply(purchaseRate).setScale(0, RoundingMode.FLOOR);
        BigDecimal discount = discountRatePercent == null || discountRatePercent.signum() == 0
                ? DEFAULT_BOND_DISCOUNT_RATE
                : discountRatePercent;
        return purchaseAmount.multiply(discount).divide(new BigDecimal("100"), 0, RoundingMode.FLOOR);
    }

    private BigDecimal resolveBondPurchaseRate(BigDecimal smp) {
        if (smp.compareTo(SMP_2천) < 0) return new BigDecimal("0.013");
        if (smp.compareTo(SMP_5천) < 0) return new BigDecimal("0.019");
        if (smp.compareTo(SMP_1억) < 0) return new BigDecimal("0.021");
        if (smp.compareTo(SMP_1억6) < 0) return new BigDecimal("0.023");
        if (smp.compareTo(SMP_2억6) < 0) return new BigDecimal("0.026");
        if (smp.compareTo(SMP_6억) < 0) return new BigDecimal("0.031");
        return new BigDecimal("0.031");
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
