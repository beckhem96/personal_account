package org.example.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.account.client.ApplyhomeClient;
import org.example.account.domain.SubscriptionRank;
import org.example.account.dto.SubscriptionItem;
import org.example.account.dto.SubscriptionsResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    /** 사용자 지정 5개 지역 — 서울 전역 + 경기 4개 시군구 */
    static final Set<String> TARGET_DISTRICTS = Set.of("의정부", "남양주", "하남", "구리");
    private static final String SEOUL_LABEL = "서울";
    private static final String APPLYHOME_DETAIL_URL = "https://www.applyhome.co.kr/ai/aia/selectAPTLttotPblancDetail.do?houseManageNo=";

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    private final ApplyhomeClient client;

    public SubscriptionsResponse findActiveToday() {
        LocalDate today = LocalDate.now();
        if (!client.isAvailable()) {
            return new SubscriptionsResponse(today, false, List.of(), List.of(), List.of());
        }

        List<SubscriptionItem> items = new ArrayList<>();
        for (JsonNode raw : client.fetchAptList()) {
            SubscriptionItem item = toItemFromApt(raw, today);
            if (item != null && !item.activeStages().isEmpty() && matchesTargetRegion(item)) {
                items.add(item);
            }
        }
        for (JsonNode raw : client.fetchRemainderList()) {
            SubscriptionItem item = toItemFromRemainder(raw, today);
            if (item != null && !item.activeStages().isEmpty() && matchesTargetRegion(item)) {
                items.add(item);
            }
        }

        return new SubscriptionsResponse(
                today,
                true,
                filterByStage(items, SubscriptionRank.FIRST),
                filterByStage(items, SubscriptionRank.SECOND),
                filterByStage(items, SubscriptionRank.REMAINDER)
        );
    }

    // 1순위/2순위는 각각 (해당지역, 경기지역, 기타지역) 3개 접수일 영역이 존재.
    // 사용자 거주지에 따라 접수일이 다르므로, 어느 영역이라도 오늘 진행 중이면 해당 순위 활성으로 본다.
    private static final String[][] RNK1_RANGES = {
            {"GNRL_RNK1_CRSPAREA_RCPTDE", "GNRL_RNK1_CRSPAREA_ENDDE"},
            {"GNRL_RNK1_ETC_GG_RCPTDE", "GNRL_RNK1_ETC_GG_ENDDE"},
            {"GNRL_RNK1_ETC_AREA_RCPTDE", "GNRL_RNK1_ETC_AREA_ENDDE"},
    };
    private static final String[][] RNK2_RANGES = {
            {"GNRL_RNK2_CRSPAREA_RCPTDE", "GNRL_RNK2_CRSPAREA_ENDDE"},
            {"GNRL_RNK2_ETC_GG_RCPTDE", "GNRL_RNK2_ETC_GG_ENDDE"},
            {"GNRL_RNK2_ETC_AREA_RCPTDE", "GNRL_RNK2_ETC_AREA_ENDDE"},
    };

    SubscriptionItem toItemFromApt(JsonNode raw, LocalDate today) {
        String houseManageNo = text(raw, "HOUSE_MANAGE_NO", "HOUSE_MNG_NO");
        String name = text(raw, "HOUSE_NM");
        if (name == null) return null;

        // 카드 표시용: 해당지역(CRSPAREA) 우선. 비어 있으면 다음 영역 폴백.
        LocalDate firstBegin = firstNonNullDate(raw, "GNRL_RNK1_CRSPAREA_RCPTDE", "GNRL_RNK1_ETC_GG_RCPTDE", "GNRL_RNK1_ETC_AREA_RCPTDE", "RCEPT_BGNDE");
        LocalDate firstEnd = firstNonNullDate(raw, "GNRL_RNK1_CRSPAREA_ENDDE", "GNRL_RNK1_ETC_GG_ENDDE", "GNRL_RNK1_ETC_AREA_ENDDE", "RCEPT_ENDDE");
        LocalDate secondBegin = firstNonNullDate(raw, "GNRL_RNK2_CRSPAREA_RCPTDE", "GNRL_RNK2_ETC_GG_RCPTDE", "GNRL_RNK2_ETC_AREA_RCPTDE");
        LocalDate secondEnd = firstNonNullDate(raw, "GNRL_RNK2_CRSPAREA_ENDDE", "GNRL_RNK2_ETC_GG_ENDDE", "GNRL_RNK2_ETC_AREA_ENDDE");

        List<SubscriptionRank> active = new ArrayList<>();
        if (anyRangeOngoing(raw, today, RNK1_RANGES)) active.add(SubscriptionRank.FIRST);
        if (anyRangeOngoing(raw, today, RNK2_RANGES)) active.add(SubscriptionRank.SECOND);

        return new SubscriptionItem(
                houseManageNo,
                name,
                text(raw, "HOUSE_SECD_NM", "HOUSE_DTL_SECD_NM"),
                text(raw, "SUBSCRPT_AREA_CODE_NM"),
                text(raw, "HSSPLY_ADRES"),
                integer(raw, "TOT_SUPLY_HSHLDCO"),
                date(raw, "RCRIT_PBLANC_DE"),
                firstBegin, firstEnd,
                secondBegin, secondEnd,
                null, null,
                active,
                pickUrl(raw, houseManageNo)
        );
    }

    private boolean anyRangeOngoing(JsonNode raw, LocalDate today, String[][] ranges) {
        for (String[] pair : ranges) {
            LocalDate begin = date(raw, pair[0]);
            LocalDate end = date(raw, pair[1]);
            if (isOngoing(today, begin, end)) return true;
        }
        return false;
    }

    private LocalDate firstNonNullDate(JsonNode raw, String... fields) {
        for (String f : fields) {
            LocalDate d = date(raw, f);
            if (d != null) return d;
        }
        return null;
    }

    SubscriptionItem toItemFromRemainder(JsonNode raw, LocalDate today) {
        String houseManageNo = text(raw, "HOUSE_MANAGE_NO", "HOUSE_MNG_NO");
        String name = text(raw, "HOUSE_NM");
        if (name == null) return null;

        LocalDate begin = date(raw, "SUBSCRPT_RCEPT_BGNDE", "GNRL_RCEPT_RCPTDE", "RCEPT_BGNDE");
        LocalDate end = date(raw, "SUBSCRPT_RCEPT_ENDDE", "GNRL_RCEPT_ENDDE", "RCEPT_ENDDE");

        List<SubscriptionRank> active = new ArrayList<>();
        if (isOngoing(today, begin, end)) active.add(SubscriptionRank.REMAINDER);

        return new SubscriptionItem(
                houseManageNo,
                name,
                text(raw, "HOUSE_SECD_NM", "HOUSE_DTL_SECD_NM"),
                text(raw, "SUBSCRPT_AREA_CODE_NM"),
                text(raw, "HSSPLY_ADRES"),
                integer(raw, "TOT_SUPLY_HSHLDCO"),
                date(raw, "RCRIT_PBLANC_DE"),
                null, null,
                null, null,
                begin, end,
                active,
                pickUrl(raw, houseManageNo)
        );
    }

    private String pickUrl(JsonNode raw, String houseManageNo) {
        String url = text(raw, "PBLANC_URL");
        return url != null ? url : detailUrl(houseManageNo);
    }

    boolean matchesTargetRegion(SubscriptionItem item) {
        String region = item.regionLabel();
        String address = item.address();
        if (region != null && region.contains(SEOUL_LABEL)) return true;
        if (address == null) return false;
        for (String district : TARGET_DISTRICTS) {
            if (address.contains(district)) return true;
        }
        return false;
    }

    static boolean isOngoing(LocalDate today, LocalDate begin, LocalDate end) {
        if (begin == null || end == null) return false;
        return !today.isBefore(begin) && !today.isAfter(end);
    }

    private List<SubscriptionItem> filterByStage(List<SubscriptionItem> items, SubscriptionRank stage) {
        List<SubscriptionItem> out = new ArrayList<>();
        for (SubscriptionItem item : items) {
            if (item.activeStages().contains(stage)) out.add(item);
        }
        return out;
    }

    private String detailUrl(String houseManageNo) {
        if (houseManageNo == null || houseManageNo.isBlank()) {
            return "https://www.applyhome.co.kr/ai/aia/selectAPTLttotPblancListView.do";
        }
        return APPLYHOME_DETAIL_URL + houseManageNo;
    }

    private static String text(JsonNode node, String... fields) {
        for (String f : fields) {
            JsonNode v = node.get(f);
            if (v != null && !v.isNull()) {
                String s = v.asText().trim();
                if (!s.isEmpty()) return s;
            }
        }
        return null;
    }

    private static Integer integer(JsonNode node, String... fields) {
        String s = text(node, fields);
        if (s == null) return null;
        try {
            return Integer.parseInt(s.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate date(JsonNode node, String... fields) {
        String s = text(node, fields);
        if (s == null) return null;
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(s, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private static List<SubscriptionItem> safeList(List<SubscriptionItem> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
