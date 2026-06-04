package org.example.account.dto;

import org.example.account.domain.LhSupplyCategory;

import java.time.LocalDate;

/** LH 분양·임대 공고 1건 */
public record LhNoticeItem(
        String panId,              // 공고ID (PAN_ID)
        String name,               // 공고명 (PAN_NM)
        String supplyTypeName,     // 공급유형명 (AIS_TP_CD_NM, 예: 행복주택/국민임대/공공분양)
        LhSupplyCategory category, // 분양/임대
        String regionLabel,        // 지역 (CNP_CD_NM)
        String noticeStatus,       // 공고상태명 (PAN_SS_NM 등)
        LocalDate noticeDate,      // 공고일 (PAN_NT_ST_DT / RCRIT_PBLANC_DE)
        LocalDate rcptBegin,       // 접수 시작일 (있을 때)
        LocalDate rcptEnd,         // 접수 마감일 (CLSG_DT 등)
        String detailUrl           // LH 청약센터 공고문 상세 URL
) {
}
