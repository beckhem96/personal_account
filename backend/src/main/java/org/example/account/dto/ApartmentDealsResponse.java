package org.example.account.dto;

import java.math.BigDecimal;
import java.util.List;

public record ApartmentDealsResponse(
        BigDecimal averagePrice,            // 권역 평균 거래가
        BigDecimal averagePricePerSqm,      // 평균 ㎡당 가격
        int totalDeals,                     // 전체 거래 건수 (필터 전)
        int filteredDeals,                  // 필터 적용 후 건수
        List<ApartmentDealDto> deals        // 필터 후 거래 목록
) {
}
