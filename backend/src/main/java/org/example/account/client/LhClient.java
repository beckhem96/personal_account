package org.example.account.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 한국토지주택공사_분양임대공고문 조회 서비스 (data.go.kr B552555/lhLeaseNoticeInfo1).
 *
 * <p>공고유형코드(UPP_AIS_TP_CD): 05 분양주택 / 06 임대주택 / 39 신혼희망타운 (그 외 01 토지 등은 제외).
 * 지역코드(CNP_CD): 11 서울특별시 / 41 경기도.
 *
 * <p>응답 구조: {@code [ {dsSch:[...]}, {resHeader:[...], dsList:[...]} ]}. dsList가 공고 행 배열.
 *
 * <p>주의: data.go.kr API는 키마다 개별 활용신청이 필요하고, 해외 IP는 지역 차단(403)될 수 있다.
 * 호출 실패는 빈 목록으로 흡수한다.
 */
@Slf4j
@Component
public class LhClient {

    /** 조회 대상 상위 공고유형코드 — 분양주택 / 임대주택 / 신혼희망타운 */
    private static final List<String> TOP_TYPE_CODES = List.of("05", "06", "39");
    /** 조회 대상 지역코드 — 서울 / 경기 (경기 시군구는 서비스 단에서 공고명으로 추가 필터) */
    private static final List<String> REGION_CODES = List.of("11", "41");
    private static final int PAGE_SIZE = 100;

    private final RestClient restClient;
    private final String apiKey;
    private final String baseUrl;

    public LhClient(
            @Qualifier("lhRestClient") RestClient restClient,
            @Value("${lh.api-key}") String apiKey,
            @Value("${lh.base-url}") String baseUrl
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 분양주택·임대주택·신혼희망타운 × 서울·경기 공고 행을 모두 모아 반환한다. */
    public List<JsonNode> fetchAll() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        List<JsonNode> all = new ArrayList<>();
        for (String top : TOP_TYPE_CODES) {
            for (String region : REGION_CODES) {
                all.addAll(fetchOne(top, region));
            }
        }
        return all;
    }

    private List<JsonNode> fetchOne(String uppAisTpCd, String cnpCd) {
        // Spring RestClient의 queryParam은 `+`/`/`/`=` reserved 문자를 인코딩하지 않아
        // data.go.kr 인증키가 깨진다. URI를 직접 인코딩해 raw로 전달.
        String query = "serviceKey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&PG_SZ=" + PAGE_SIZE
                + "&PAGE=1"
                + "&UPP_AIS_TP_CD=" + uppAisTpCd
                + "&CNP_CD=" + cnpCd;
        URI uri = URI.create(baseUrl + "/lhLeaseNoticeInfo1?" + query);

        try {
            JsonNode response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return Collections.emptyList();
            }
            List<JsonNode> rows = new ArrayList<>();
            collectNoticeRows(response, rows);
            return rows;
        } catch (Exception e) {
            log.warn("LH 공고 API 호출 실패 UPP_AIS_TP_CD={} CNP_CD={} : {}", uppAisTpCd, cnpCd, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 응답 트리에서 PAN_NM/PAN_ID를 가진 객체 배열(=dsList)을 찾아 행으로 수집한다.
     * resHeader([SS_CODE..])·dsSch([PAGE..])는 해당 필드가 없어 자연히 걸러진다.
     */
    private void collectNoticeRows(JsonNode node, List<JsonNode> out) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            if (node.size() > 0 && node.get(0).isObject() && looksLikeNotice(node.get(0))) {
                node.forEach(out::add);
                return;
            }
            node.forEach(child -> collectNoticeRows(child, out));
        } else if (node.isObject()) {
            node.forEach(child -> collectNoticeRows(child, out));
        }
    }

    private boolean looksLikeNotice(JsonNode obj) {
        return obj.has("PAN_NM") || obj.has("PAN_ID") || obj.has("AIS_TP_CD_NM");
    }
}
