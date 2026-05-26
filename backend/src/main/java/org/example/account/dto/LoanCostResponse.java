package org.example.account.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public record LoanCostResponse(
        BigDecimal mortgageRegistrationCost, // 근저당설정비 (채권최고액 × 0.24%)
        BigDecimal stampDuty,                // 인지세 (대출액별 차등)
        BigDecimal appraisalFee,             // 감정평가수수료
        BigDecimal totalCost,                // 총 부대비용
        Map<String, BigDecimal> breakdown
) {
    public static LoanCostResponse of(BigDecimal mortgage, BigDecimal stamp, BigDecimal appraisal) {
        BigDecimal total = mortgage.add(stamp).add(appraisal);
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        breakdown.put("근저당설정비", mortgage);
        breakdown.put("인지세", stamp);
        breakdown.put("감정평가수수료", appraisal);
        return new LoanCostResponse(mortgage, stamp, appraisal, total, breakdown);
    }
}
