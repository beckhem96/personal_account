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
 * 분양(UPP_AIS_TP_CD=05) / 임대(06) 공고 목록을 가져온다.
 *
 * <p>주의: data.go.kr API는 키마다 개별 활용신청이 필요하다. 미신청 키로 호출하면
 * 본문에 "Forbidden"(HTTP 403)이 내려온다. 이 경우 빈 목록으로 처리하고 경고만 남긴다.
 */
@Slf4j
@Component
public class LhClient {

    /** 분양 상위유형코드 */
    private static final String UPP_SALE = "05";
    /** 임대 상위유형코드 */
    private static final String UPP_RENT = "06";
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

    /** 분양 공고 목록 */
    public List<JsonNode> fetchSaleNotices() {
        return fetch(UPP_SALE);
    }

    /** 임대 공고 목록 */
    public List<JsonNode> fetchRentNotices() {
        return fetch(UPP_RENT);
    }

    private List<JsonNode> fetch(String uppAisTpCd) {
        if (!isAvailable()) {
            return Collections.emptyList();
        }

        // Spring RestClient의 queryParam은 `+`/`/`/`=` reserved 문자를 인코딩하지 않아
        // data.go.kr 인증키가 깨진다(=등록되지 않은 인증키). URI를 직접 인코딩해 raw로 전달.
        String query = "serviceKey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&PG_SZ=" + PAGE_SIZE
                + "&PAGE=1"
                + "&UPP_AIS_TP_CD=" + uppAisTpCd;
        URI uri = URI.create(baseUrl + "/lhLeaseNoticeInfo1?" + query);

        try {
            JsonNode response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return Collections.emptyList();
            }
            return extractRows(response);
        } catch (Exception e) {
            // 활용신청 미완료(Forbidden)·일시 장애 등은 빈 목록으로 흡수
            log.warn("LH 공고 API 호출 실패 uppAisTpCd={} : {}", uppAisTpCd, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * LH B552555 응답은 보통 {@code [ {resHeader:[...]}, {dsList:[...]} ]} 형태다.
     * 구조가 유동적이라, PAN_NM/PAN_ID를 가진 객체 배열을 트리에서 찾아 행 목록으로 반환한다.
     */
    private List<JsonNode> extractRows(JsonNode root) {
        List<JsonNode> rows = new ArrayList<>();
        collectNoticeRows(root, rows);
        return rows;
    }

    private void collectNoticeRows(JsonNode node, List<JsonNode> out) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            boolean isNoticeRowArray = node.size() > 0
                    && node.get(0).isObject()
                    && looksLikeNotice(node.get(0));
            if (isNoticeRowArray) {
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
