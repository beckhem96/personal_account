package org.example.account.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public record AcquisitionCostResponse(
        BigDecimal acquisitionTax,          // 취득세 본세
        BigDecimal localEducationTax,       // 지방교육세
        BigDecimal ruralSpecialTax,         // 농어촌특별세 (85㎡ 초과 시)
        BigDecimal brokerFee,               // 중개수수료
        BigDecimal judicialFee,             // 법무사 보수
        BigDecimal stampDuty,               // 인지세
        BigDecimal nationalHousingBondLoss, // 국민주택채권 할인손실
        BigDecimal totalCost,               // 총 부대비용 합계
        Map<String, BigDecimal> breakdown   // 라벨 → 금액 (PieChart용)
) {
    public static AcquisitionCostResponse of(BigDecimal acquisitionTax, BigDecimal localEducationTax,
                                             BigDecimal ruralSpecialTax, BigDecimal brokerFee,
                                             BigDecimal judicialFee, BigDecimal stampDuty,
                                             BigDecimal nationalHousingBondLoss) {
        BigDecimal total = acquisitionTax
                .add(localEducationTax)
                .add(ruralSpecialTax)
                .add(brokerFee)
                .add(judicialFee)
                .add(stampDuty)
                .add(nationalHousingBondLoss);

        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        breakdown.put("취득세", acquisitionTax);
        breakdown.put("지방교육세", localEducationTax);
        breakdown.put("농어촌특별세", ruralSpecialTax);
        breakdown.put("중개수수료", brokerFee);
        breakdown.put("법무사 보수", judicialFee);
        breakdown.put("인지세", stampDuty);
        breakdown.put("국민주택채권 할인손실", nationalHousingBondLoss);

        return new AcquisitionCostResponse(acquisitionTax, localEducationTax, ruralSpecialTax,
                brokerFee, judicialFee, stampDuty, nationalHousingBondLoss, total, breakdown);
    }
}
