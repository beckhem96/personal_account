package org.example.account.dto;

import java.time.LocalDate;
import java.util.List;

/** LH 공공분양·임대 청약 조회 응답 — 분양/임대 그룹 분리 */
public record LhNoticesResponse(
        LocalDate asOf,
        boolean apiKeyConfigured,
        List<LhNoticeItem> sale,   // 분양
        List<LhNoticeItem> rent    // 임대
) {
}
