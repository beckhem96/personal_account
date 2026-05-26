package org.example.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApartmentDealDto(
        String apartmentName,
        BigDecimal dealAmount,      // 거래금액 (원)
        BigDecimal exclusiveArea,   // 전용면적 (㎡)
        Integer floor,
        Integer buildYear,
        LocalDate dealDate,
        String dong,                // 법정동명
        String lawdCd               // 법정동 코드 (5자리)
) {
}
