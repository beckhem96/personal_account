package org.example.account.service;

import org.example.account.domain.HouseCount;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HousingTaxServiceTest {

    private final HousingTaxService service = new HousingTaxService();

    @Test
    void 매매가_6억_1주택_85m2_이하() {
        // 6억 × 1% = 600만, 농특세 없음(85↓), 지방교육세 6억×0.1% = 60만
        var r = service.calculate(new BigDecimal("600000000"), HouseCount.SINGLE, false, false, new BigDecimal("84"));
        assertThat(r.acquisitionTax()).isEqualByComparingTo("6000000");
        assertThat(r.ruralSpecialTax()).isEqualByComparingTo("0");
        assertThat(r.localEducationTax()).isEqualByComparingTo("600000");
        assertThat(r.firstTimeDiscount()).isEqualByComparingTo("0");
    }

    @Test
    void 매매가_9억_1주택_85m2_초과() {
        // 9억은 3% 적용. 본세 = 9억×3% = 2700만, 농특세 = 9억×0.2% = 180만, 지방교육세 = 9억×0.3% = 270만
        var r = service.calculate(new BigDecimal("900000000"), HouseCount.SINGLE, false, false, new BigDecimal("100"));
        assertThat(r.acquisitionTax()).isEqualByComparingTo("27000000");
        assertThat(r.ruralSpecialTax()).isEqualByComparingTo("1800000");
        assertThat(r.localEducationTax()).isEqualByComparingTo("2700000");
    }

    @Test
    void 매매가_9억_다주택_조정지역_3주택_85m2_초과() {
        // 3주택 + 조정지역 = 12%. 본세 = 9억×12% = 1.08억, 농특세 = 9억×1% = 900만, 지방교육세 = 9억×0.4% = 360만
        var r = service.calculate(new BigDecimal("900000000"), HouseCount.THREE_OR_MORE, true, false, new BigDecimal("100"));
        assertThat(r.acquisitionTax()).isEqualByComparingTo("108000000");
        assertThat(r.ruralSpecialTax()).isEqualByComparingTo("9000000");
        assertThat(r.localEducationTax()).isEqualByComparingTo("3600000");
    }

    @Test
    void 매매가_9억_다주택_조정지역_2주택() {
        // 2주택 + 조정지역 = 8%. 본세 = 9억×8% = 7200만
        var r = service.calculate(new BigDecimal("900000000"), HouseCount.TWO, true, false, new BigDecimal("84"));
        assertThat(r.acquisitionTax()).isEqualByComparingTo("72000000");
        assertThat(r.ruralSpecialTax()).isEqualByComparingTo("0"); // 85↓ → 농특세 면제
    }

    @Test
    void 생애최초_5억_1주택_감면_적용() {
        // 5억 × 1% = 500만, 200만 한도 감면 → 300만
        var r = service.calculate(new BigDecimal("500000000"), HouseCount.SINGLE, false, true, new BigDecimal("60"));
        assertThat(r.firstTimeDiscount()).isEqualByComparingTo("2000000");
        assertThat(r.acquisitionTax()).isEqualByComparingTo("3000000");
    }

    @Test
    void 생애최초_13억_1주택_감면_미적용() {
        // 12억 초과 → 감면 불가. 본세 = 13억×3% = 3900만 그대로
        var r = service.calculate(new BigDecimal("1300000000"), HouseCount.SINGLE, false, true, new BigDecimal("84"));
        assertThat(r.firstTimeDiscount()).isEqualByComparingTo("0");
        assertThat(r.acquisitionTax()).isEqualByComparingTo("39000000");
    }

    @Test
    void 누진_영역_7억5천만_1주택() {
        // 누진식: rate = ((7.5 × 2/3) - 3) / 100 = (5 - 3) / 100 = 2%
        var r = service.calculate(new BigDecimal("750000000"), HouseCount.SINGLE, false, false, new BigDecimal("84"));
        // 본세 = 7.5억 × 2% = 1500만
        assertThat(r.acquisitionTax()).isEqualByComparingTo("15000000");
        assertThat(r.appliedRate()).isEqualByComparingTo("0.02");
    }
}
