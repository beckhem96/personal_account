package org.example.account.dto;

import org.example.account.domain.SubscriptionRank;

import java.time.LocalDate;
import java.util.List;

public record SubscriptionItem(
        String houseManageNo,
        String name,
        String houseType,         // 주택구분 (예: 아파트, 도시형생활주택)
        String regionLabel,       // 공급지역
        String address,           // 공급위치
        Integer totalSupplyHouseholds,
        LocalDate noticeDate,
        LocalDate firstRcptBegin,
        LocalDate firstRcptEnd,
        LocalDate secondRcptBegin,
        LocalDate secondRcptEnd,
        LocalDate remainderRcptBegin,
        LocalDate remainderRcptEnd,
        List<SubscriptionRank> activeStages,
        String applyhomeUrl
) {
}
