package org.example.account.dto;

import java.util.List;

public record RegionInfo(
        String code,        // 법정동 코드 5자리 (시군구 단위)
        String name,        // 시군구명 (예: "강남구")
        String parentName   // 광역(서울/경기/인천)
) {
    public record RegionTree(
            String region,           // SEOUL / GYEONGGI / INCHEON
            String regionLabel,      // 서울특별시 / 경기도 / 인천광역시
            List<RegionInfo> districts
    ) {
    }
}
