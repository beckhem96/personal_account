package org.example.account.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.account.dto.ApartmentDealDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 국토교통부 아파트 매매 실거래가 공공 API 클라이언트.
 * 응답: XML, 필드명 한글. 거래금액 단위: 만원 (콤마/공백 포함 문자열).
 */
@Slf4j
@Component
public class MolitClient {

    private static final BigDecimal MAN_WON = new BigDecimal("10000");
    private final XmlMapper xmlMapper = new XmlMapper();

    private final RestClient restClient;
    private final String apiKey;

    public MolitClient(
            @Qualifier("molitRestClient") RestClient restClient,
            @Value("${molit.api-key}") String apiKey
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    /** 키 발급 전에는 빈 결과 반환. */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 특정 시군구(법정동 5자리) × 특정 년월의 거래 목록 조회.
     * dealYearMonth: yyyyMM 형식 (예: "202604")
     */
    public List<ApartmentDealDto> fetchDeals(String lawdCd, String dealYearMonth) {
        if (!isAvailable()) {
            log.info("MOLIT API 키 미설정 — 빈 결과 반환 (lawdCd={}, ym={})", lawdCd, dealYearMonth);
            return List.of();
        }
        String xml;
        try {
            xml = restClient.get()
                    .uri(uri -> uri
                            .queryParam("serviceKey", apiKey)
                            .queryParam("LAWD_CD", lawdCd)
                            .queryParam("DEAL_YMD", dealYearMonth)
                            .queryParam("numOfRows", "1000")
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.warn("MOLIT API 호출 실패 lawdCd={} ym={} status={} body={}",
                    lawdCd, dealYearMonth, e.getStatusCode().value(), e.getMessage());
            return List.of();
        }

        if (xml == null || xml.isBlank()) return List.of();
        return parseDeals(xml, lawdCd);
    }

    private List<ApartmentDealDto> parseDeals(String xml, String lawdCd) {
        List<ApartmentDealDto> result = new ArrayList<>();
        try {
            JsonNode root = xmlMapper.readTree(xml.getBytes());
            JsonNode header = root.path("header");
            String code = header.path("resultCode").asText("");
            if (!"000".equals(code) && !code.isEmpty()) {
                log.warn("MOLIT API 비정상 응답 code={} msg={}", code, header.path("resultMsg").asText(""));
                return result;
            }
            JsonNode items = root.path("body").path("items").path("item");
            if (items.isMissingNode()) return result;

            if (items.isArray()) {
                for (JsonNode item : items) result.add(toDto(item, lawdCd));
            } else if (items.isObject()) {
                // 단일 거래일 경우 array가 아닌 object로 옴
                result.add(toDto(items, lawdCd));
            }
        } catch (Exception e) {
            log.warn("MOLIT XML 파싱 실패: {}", e.getMessage());
        }
        return result;
    }

    private ApartmentDealDto toDto(JsonNode item, String lawdCd) {
        String dealAmountRaw = textOr(item, "거래금액", "0");
        BigDecimal manWon = new BigDecimal(dealAmountRaw.replaceAll("[ ,]", ""));
        BigDecimal won = manWon.multiply(MAN_WON);

        int year = item.path("년").asInt(0);
        int month = item.path("월").asInt(0);
        int day = item.path("일").asInt(0);
        LocalDate dealDate = (year > 0 && month > 0 && day > 0) ? LocalDate.of(year, month, day) : null;

        BigDecimal area = item.path("전용면적").isMissingNode()
                ? BigDecimal.ZERO
                : new BigDecimal(item.path("전용면적").asText("0"));

        return new ApartmentDealDto(
                textOr(item, "아파트", ""),
                won,
                area,
                item.path("층").asInt(0),
                item.path("건축년도").asInt(0),
                dealDate,
                textOr(item, "법정동", "").trim(),
                lawdCd
        );
    }

    private String textOr(JsonNode node, String field, String def) {
        JsonNode v = node.path(field);
        return v.isMissingNode() ? def : v.asText(def);
    }
}
