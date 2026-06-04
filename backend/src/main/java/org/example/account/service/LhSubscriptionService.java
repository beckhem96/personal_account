package org.example.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.example.account.client.LhClient;
import org.example.account.domain.LhSupplyCategory;
import org.example.account.dto.LhNoticeItem;
import org.example.account.dto.LhNoticesResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * LH 공공분양·임대 공고를 조회해 사용자 지정 5개 지역(서울 + 의정부/남양주/하남/구리)의
 * "오늘 접수 가능한" 공고만 분양/임대로 나눠 반환한다.
 *
 * <p>분양(UPP=05)은 전부 포함, 임대(UPP=06)는 주요 유형(행복주택·국민임대·영구임대)만 포함하고
 * 매입임대·전세임대 등 수시 공고는 제외한다.
 */
@Service
@RequiredArgsConstructor
public class LhSubscriptionService {

    /** 사용자 지정 5개 지역 — 서울 전역 + 경기 4개 시군구 */
    static final String SEOUL_LABEL = "서울";
    static final Set<String> TARGET_DISTRICTS = Set.of("의정부", "남양주", "하남", "구리");

    /** 임대 공고 중 노출할 유형(이름 부분일치). 매입임대·전세임대는 자연히 제외된다. */
    static final Set<String> RENT_ALLOWED_TYPES = Set.of("행복주택", "국민임대", "영구임대");

    private static final String LH_DETAIL_URL = "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancInfo.do";
    private static final String LH_LIST_URL = "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1027";

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    private final LhClient client;

    public LhNoticesResponse findActiveToday() {
        LocalDate today = LocalDate.now();
        if (!client.isAvailable()) {
            return new LhNoticesResponse(today, false, List.of(), List.of());
        }

        List<LhNoticeItem> sale = new ArrayList<>();
        for (JsonNode raw : client.fetchSaleNotices()) {
            LhNoticeItem item = toItem(raw, LhSupplyCategory.SALE, today);
            if (item != null && matchesTargetRegion(item)) {
                sale.add(item);
            }
        }

        List<LhNoticeItem> rent = new ArrayList<>();
        for (JsonNode raw : client.fetchRentNotices()) {
            LhNoticeItem item = toItem(raw, LhSupplyCategory.RENT, today);
            if (item != null && matchesRentType(item) && matchesTargetRegion(item)) {
                rent.add(item);
            }
        }

        return new LhNoticesResponse(today, true, sale, rent);
    }

    LhNoticeItem toItem(JsonNode raw, LhSupplyCategory category, LocalDate today) {
        String name = text(raw, "PAN_NM", "AIS_TP_CD_NM");
        if (name == null) {
            return null;
        }

        LocalDate rcptBegin = date(raw, "SUBSCRT_RCPT_BGNDE", "RCRIT_RCPT_BGNDE", "RCPT_BGNDE");
        LocalDate rcptEnd = date(raw, "SUBSCRT_RCPT_ENDDE", "RCRIT_RCPT_ENDDE", "RCPT_ENDDE", "CLSG_DT");

        // 접수중 판정: 접수 시작/마감일이 있으면 기간 검사, 없으면 마감일만 미래인지로 보수적 판정.
        if (!isReceivable(today, rcptBegin, rcptEnd)) {
            return null;
        }

        return new LhNoticeItem(
                text(raw, "PAN_ID"),
                name,
                text(raw, "AIS_TP_CD_NM"),
                category,
                text(raw, "CNP_CD_NM", "ARA_NM", "CNP_CD"),
                text(raw, "PAN_SS_NM", "PAN_NT_ST_CD_NM", "PAN_NT_ST_CD"),
                date(raw, "PAN_NT_ST_DT", "RCRIT_PBLANC_DE", "PAN_DT"),
                rcptBegin,
                rcptEnd,
                pickUrl(raw)
        );
    }

    /** 접수 마감일이 오늘 이후(또는 오늘)이고, 시작일이 있으면 이미 시작된 공고만 통과. */
    static boolean isReceivable(LocalDate today, LocalDate begin, LocalDate end) {
        if (end != null && today.isAfter(end)) {
            return false;
        }
        if (begin != null && today.isBefore(begin)) {
            return false;
        }
        // 마감일조차 모르면(목록 API 한계) 일단 노출하고 상세 링크로 확인하도록 둔다.
        return true;
    }

    boolean matchesRentType(LhNoticeItem item) {
        String type = item.supplyTypeName();
        if (type == null) {
            return false;
        }
        for (String allowed : RENT_ALLOWED_TYPES) {
            if (type.contains(allowed)) {
                return true;
            }
        }
        return false;
    }

    boolean matchesTargetRegion(LhNoticeItem item) {
        String haystack = (nullToEmpty(item.regionLabel()) + " " + nullToEmpty(item.name()));
        if (haystack.contains(SEOUL_LABEL)) {
            return true;
        }
        for (String district : TARGET_DISTRICTS) {
            if (haystack.contains(district)) {
                return true;
            }
        }
        return false;
    }

    private String pickUrl(JsonNode raw) {
        String url = text(raw, "DTL_URL", "PAN_DT_URL");
        if (url != null) {
            return url;
        }
        String panId = text(raw, "PAN_ID");
        if (panId == null) {
            return LH_LIST_URL;
        }
        StringBuilder sb = new StringBuilder(LH_DETAIL_URL).append("?panId=").append(panId);
        appendParam(sb, "ccrCnntSysDsCd", text(raw, "CCR_CNNT_SYS_DS_CD"));
        appendParam(sb, "uppAisTpCd", text(raw, "UPP_AIS_TP_CD"));
        appendParam(sb, "aisTpCd", text(raw, "AIS_TP_CD"));
        sb.append("&mi=1026");
        return sb.toString();
    }

    private static void appendParam(StringBuilder sb, String key, String value) {
        if (value != null && !value.isBlank()) {
            sb.append('&').append(key).append('=').append(value);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String text(JsonNode node, String... fields) {
        for (String f : fields) {
            JsonNode v = node.get(f);
            if (v != null && !v.isNull()) {
                String s = v.asText().trim();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        }
        return null;
    }

    private static LocalDate date(JsonNode node, String... fields) {
        String s = text(node, fields);
        if (s == null) {
            return null;
        }
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(s, fmt);
            } catch (Exception ignored) {
                // 다음 포맷 시도
            }
        }
        return null;
    }
}
