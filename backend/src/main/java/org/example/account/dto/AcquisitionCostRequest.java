package org.example.account.dto;

import org.example.account.domain.HouseCount;

import java.math.BigDecimal;

public record AcquisitionCostRequest(
        BigDecimal salePrice,           // 매매가
        HouseCount houseCount,          // 보유 주택수 (취득 후 기준)
        Boolean isRegulatedArea,        // 조정대상지역 여부
        Boolean isFirstTime,            // 생애최초 구입 여부
        BigDecimal exclusiveAreaSqm,    // 전용면적 (㎡) — 85㎡ 초과 시 농특세 부과
        BigDecimal standardMarketPrice, // 시가표준액 (국민주택채권 매입 기준, optional)
        BigDecimal bondDiscountRate     // 국민주택채권 할인율 (%, 사용자 직접 입력)
) {
}
