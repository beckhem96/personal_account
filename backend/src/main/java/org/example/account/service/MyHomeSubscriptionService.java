package org.example.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.example.account.client.MyHomeClient;
import org.example.account.domain.LhSupplyCategory;
import org.example.account.dto.LhNoticeItem;
import org.example.account.dto.LhNoticesResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MyHomeSubscriptionService {

    private static final String SEOUL_LABEL = "서울";
    private static final Set<String> TARGET_DISTRICTS = Set.of("의정부", "남양주", "하남", "구리", "용인", "수원", "김포");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    private final MyHomeClient myHomeClient;

    public LhNoticesResponse findShActiveToday() {
        return fetchAndFilter("서울주택도시공사");
    }

    public LhNoticesResponse findGhActiveToday() {
        return fetchAndFilter("경기주택도시공사");
    }

    private LhNoticesResponse fetchAndFilter(String targetInstitution) {
        LocalDate today = LocalDate.now();
        if (!myHomeClient.isAvailable()) {
            return new LhNoticesResponse(today, false, Collections.emptyList(), Collections.emptyList());
        }

        List<LhNoticeItem> sale = new ArrayList<>();
        List<LhNoticeItem> rent = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (JsonNode raw : myHomeClient.fetchAll()) {
            String inst = text(raw, "suplyInsttNm");
            if (inst == null || !inst.contains(targetInstitution)) {
                continue;
            }

            LhNoticeItem item = toItem(raw, today);
            if (item == null || !matchesTargetRegion(item)) {
                continue;
            }

            String dedupKey = item.panId() != null ? item.panId() : item.name();
            if (!seen.add(dedupKey)) {
                continue;
            }

            if (item.category() == LhSupplyCategory.RENT) {
                rent.add(item);
            } else {
                sale.add(item);
            }
        }

        return new LhNoticesResponse(today, true, sale, rent);
    }

    LhNoticeItem toItem(JsonNode raw, LocalDate today) {
        String name = text(raw, "pblancNm");
        if (name == null) {
            return null;
        }

        LocalDate beginDate = date(raw, "rceptBgnde", "beginDe");
        LocalDate endDate = date(raw, "rceptEndde", "endDe");
        LocalDate noticeDate = date(raw, "rcritNtcDe", "rcritPblancDe");

        if (endDate != null && today.isAfter(endDate)) {
            return null;
        }

        String status = "공고중";
        if (beginDate != null && endDate != null) {
            if (!today.isBefore(beginDate) && !today.isAfter(endDate)) {
                status = "접수중";
            }
        }

        String typeName = text(raw, "uppAisTpCdNm", "suplyTyNm", "houseTyNm");
        LhSupplyCategory category = LhSupplyCategory.SALE;
        if (typeName != null && (typeName.contains("임대") || typeName.contains("RENT"))) {
            category = LhSupplyCategory.RENT;
        }

        return new LhNoticeItem(
                text(raw, "hssplyAdres", "fullAdres"),
                name,
                typeName != null ? typeName : "공공주택",
                category,
                text(raw, "suplyInsttNm"),
                status,
                noticeDate,
                beginDate,
                endDate,
                text(raw, "pblancUrl", "url", "pcUrl")
        );
    }

    private boolean matchesTargetRegion(LhNoticeItem item) {
        String haystack = nullToEmpty(item.regionLabel()) + " " + nullToEmpty(item.name());
        if (item.panId() != null) {
            haystack += " " + item.panId();
        }
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
            }
        }
        return null;
    }
}
