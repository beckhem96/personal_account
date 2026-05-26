package org.example.account.service;

import org.example.account.domain.HouseCount;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 주택 취득세 + 지방교육세 + 농어촌특별세 계산.
 * 2025년 말 기준 지방세법 규정을 단순화해 구현. 세율표는 본 클래스 상단 상수로 모아 관리.
 */
@Service
public class HousingTaxService {

    private static final BigDecimal PRICE_6E = new BigDecimal("600000000");
    private static final BigDecimal PRICE_9E = new BigDecimal("900000000");
    private static final BigDecimal PRICE_12E = new BigDecimal("1200000000");
    private static final BigDecimal AREA_85 = new BigDecimal("85");

    private static final BigDecimal RATE_BASE_LOW = new BigDecimal("0.01");       // 6억 이하 1%
    private static final BigDecimal RATE_BASE_HIGH = new BigDecimal("0.03");      // 9억 초과 3%
    private static final BigDecimal RATE_MULTI_REG_2 = new BigDecimal("0.08");    // 2주택 조정 8%
    private static final BigDecimal RATE_MULTI_REG_3 = new BigDecimal("0.12");    // 3주택↑ 조정 12%
    private static final BigDecimal RATE_MULTI_NONREG_3 = new BigDecimal("0.08"); // 3주택↑ 비조정 8%

    private static final BigDecimal FIRST_TIME_CAP = new BigDecimal("2000000");   // 생애최초 200만 한도

    // 농특세 (85㎡ 초과 시)
    private static final BigDecimal RST_SINGLE = new BigDecimal("0.002");         // 1주택 0.2%
    private static final BigDecimal RST_MULTI_8 = new BigDecimal("0.006");        // 다주택 8% 케이스 0.6%
    private static final BigDecimal RST_MULTI_12 = new BigDecimal("0.01");        // 다주택 12% 케이스 1.0%

    // 지방교육세
    private static final BigDecimal LET_MULTI = new BigDecimal("0.004");          // 다주택 정액 0.4%
    private static final BigDecimal LET_BASE_FACTOR = new BigDecimal("0.1");      // 1주택은 본세율의 10%

    public record TaxBreakdown(
            BigDecimal acquisitionTax,
            BigDecimal localEducationTax,
            BigDecimal ruralSpecialTax,
            BigDecimal appliedRate,           // 적용 본세율 (참고용)
            BigDecimal firstTimeDiscount      // 생애최초 감면액
    ) {}

    public TaxBreakdown calculate(BigDecimal salePrice, HouseCount houseCount,
                                  boolean isRegulatedArea, boolean isFirstTime,
                                  BigDecimal exclusiveAreaSqm) {
        BigDecimal price = nz(salePrice);
        BigDecimal area = nz(exclusiveAreaSqm);

        BigDecimal rate = resolveBaseRate(price, houseCount, isRegulatedArea);
        BigDecimal baseTax = price.multiply(rate).setScale(0, RoundingMode.FLOOR);

        // 생애최초 감면: 12억 이하 + 1주택 케이스에서만 적용 (소득요건은 본 계산에서 생략)
        BigDecimal discount = BigDecimal.ZERO;
        if (isFirstTime && houseCount == HouseCount.SINGLE && price.compareTo(PRICE_12E) <= 0) {
            discount = baseTax.min(FIRST_TIME_CAP);
            baseTax = baseTax.subtract(discount);
        }

        BigDecimal localEducationTax = calculateLocalEducationTax(price, rate, houseCount, isRegulatedArea);
        BigDecimal ruralSpecialTax = calculateRuralSpecialTax(price, area, houseCount, isRegulatedArea);

        return new TaxBreakdown(baseTax, localEducationTax, ruralSpecialTax, rate, discount);
    }

    private BigDecimal resolveBaseRate(BigDecimal price, HouseCount houseCount, boolean isRegulatedArea) {
        if (houseCount == HouseCount.SINGLE) {
            return singleHouseRate(price);
        }
        if (houseCount == HouseCount.TWO) {
            if (isRegulatedArea) return RATE_MULTI_REG_2;
            return singleHouseRate(price); // 비조정지역 2주택은 1주택과 동일 누진
        }
        // 3주택 이상
        return isRegulatedArea ? RATE_MULTI_REG_3 : RATE_MULTI_NONREG_3;
    }

    /**
     * 1주택 누진세율. 6억 이하 1%, 9억 초과 3%, 6~9억 구간은 직선 누진.
     * 공식: rate = ((매매가/1억) × 2/3 - 3) / 100
     */
    private BigDecimal singleHouseRate(BigDecimal price) {
        if (price.compareTo(PRICE_6E) <= 0) return RATE_BASE_LOW;
        if (price.compareTo(PRICE_9E) >= 0) return RATE_BASE_HIGH;
        BigDecimal hundredMil = new BigDecimal("100000000");
        BigDecimal priceInHundredMil = price.divide(hundredMil, MathContext.DECIMAL64);
        BigDecimal percent = priceInHundredMil.multiply(new BigDecimal("2"))
                .divide(new BigDecimal("3"), MathContext.DECIMAL64)
                .subtract(new BigDecimal("3"));
        return percent.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLocalEducationTax(BigDecimal price, BigDecimal baseRate,
                                                   HouseCount houseCount, boolean isRegulatedArea) {
        boolean isMultiHigh = (houseCount == HouseCount.TWO && isRegulatedArea)
                || houseCount == HouseCount.THREE_OR_MORE;
        if (isMultiHigh) {
            return price.multiply(LET_MULTI).setScale(0, RoundingMode.FLOOR);
        }
        BigDecimal letRate = baseRate.multiply(LET_BASE_FACTOR);
        return price.multiply(letRate).setScale(0, RoundingMode.FLOOR);
    }

    private BigDecimal calculateRuralSpecialTax(BigDecimal price, BigDecimal area,
                                                 HouseCount houseCount, boolean isRegulatedArea) {
        if (area.compareTo(AREA_85) <= 0) return BigDecimal.ZERO;
        BigDecimal rate;
        if (houseCount == HouseCount.THREE_OR_MORE && isRegulatedArea) {
            rate = RST_MULTI_12;
        } else if ((houseCount == HouseCount.TWO && isRegulatedArea)
                || (houseCount == HouseCount.THREE_OR_MORE)) {
            rate = RST_MULTI_8;
        } else {
            rate = RST_SINGLE;
        }
        return price.multiply(rate).setScale(0, RoundingMode.FLOOR);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
