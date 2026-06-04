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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * LH 분양·임대 공고를 조회해 사용자 지정 5개 지역(서울 + 의정부/남양주/하남/구리)의
 * "접수 마감 전" 공고만 분양/임대로 나눠 반환한다.
 *
 * <p>분양(05)·신혼희망타운(39)은 전부 포함, 임대(06)는 주요 유형(행복주택·국민임대·영구임대)만 포함한다.
 * 접수마감(PAN_SS)이거나 마감일(CLSG_DT)이 지난 공고는 제외한다.
 */
@Service
@RequiredArgsConstructor
public class LhSubscriptionService {

    static final String SEOUL_LABEL = "서울";
    static final Set<String> TARGET_DISTRICTS = Set.of("의정부", "남양주", "하남", "구리", "용인", "수원", "김포");

    /** 임대(06) 공고 중 노출할 세부유형(AIS_TP_CD_NM 부분일치). 매입임대·전세임대·장기전세 등은 제외. */
    static final Set<String> RENT_ALLOWED_TYPES = Set.of("행복주택", "국민임대", "영구임대");

    /** 임대 상위유형코드 */
    private static final String UPP_RENT = "06";
    /** 제외할 공고상태 */
    private static final String STATUS_CLOSED = "접수마감";

    private static final String LH_LIST_URL = "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1027";

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    private final LhClient client;

    public LhNoticesResponse findActiveToday() {
        LocalDate today = LocalDate.now();
        if (!client.isAvailable()) {
            return new LhNoticesResponse(today, false, List.of(), List.of());
        }

        List<LhNoticeItem> sale = new ArrayList<>();
        List<LhNoticeItem> rent = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (JsonNode raw : client.fetchAll()) {
            LhNoticeItem item = toItem(raw, today);
            if (item == null || !matchesTargetRegion(item)) {
                continue;
            }
            // 같은 공고(PAN_ID)가 조합별로 중복될 수 있어 1회만
            String dedupKey = item.panId() != null ? item.panId() : item.name();
            if (!seen.add(dedupKey)) {
                continue;
            }
            if (item.category() == LhSupplyCategory.RENT) {
                if (matchesRentType(item)) {
                    rent.add(item);
                }
            } else {
                sale.add(item);
            }
        }

        return new LhNoticesResponse(today, true, sale, rent);
    }

    LhNoticeItem toItem(JsonNode raw, LocalDate today) {
        String name = text(raw, "PAN_NM");
        if (name == null) {
            return null;
        }

        String status = text(raw, "PAN_SS");
        LocalDate closeDate = date(raw, "CLSG_DT");
        // 접수마감 상태이거나 마감일이 지난 공고는 제외
        if (STATUS_CLOSED.equals(status)) {
            return null;
        }
        if (closeDate != null && today.isAfter(closeDate)) {
            return null;
        }

        return new LhNoticeItem(
                text(raw, "PAN_ID"),
                name,
                text(raw, "AIS_TP_CD_NM", "UPP_AIS_TP_NM"),
                categoryOf(raw),
                text(raw, "CNP_CD_NM"),
                status,
                date(raw, "PAN_NT_ST_DT", "PAN_DT"),
                null,               // 목록 API는 접수 시작일을 제공하지 않음
                closeDate,          // 마감일
                pickUrl(raw)
        );
    }

    private LhSupplyCategory categoryOf(JsonNode raw) {
        String upp = text(raw, "UPP_AIS_TP_CD");
        return UPP_RENT.equals(upp) ? LhSupplyCategory.RENT : LhSupplyCategory.SALE;
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
        String haystack = nullToEmpty(item.regionLabel()) + " " + nullToEmpty(item.name());
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
        String url = text(raw, "DTL_URL", "DTL_URL_MOB");
        return url != null ? url : LH_LIST_URL;
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
