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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ApplyhomeClient {

    private static final int PER_PAGE = 1000;
    /** 모집공고일 기준 최근 N일치만 조회 (접수기간이 보통 공고 후 2~3주 내) */
    private static final int LOOKBACK_DAYS = 60;

    private final RestClient restClient;
    private final String apiKey;
    private final String baseUrl;
    private final String listPath;
    private final String remndrPath;

    public ApplyhomeClient(
            @Qualifier("applyhomeRestClient") RestClient restClient,
            @Value("${applyhome.api-key}") String apiKey,
            @Value("${applyhome.base-url}") String baseUrl,
            @Value("${applyhome.list-path}") String listPath,
            @Value("${applyhome.remndr-path}") String remndrPath
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.listPath = listPath;
        this.remndrPath = remndrPath;
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** APT 일반청약 공고 목록 (최근순, 최대 PER_PAGE건) */
    public List<JsonNode> fetchAptList() {
        return fetchData(listPath);
    }

    /** APT 무순위/잔여세대 공고 목록 */
    public List<JsonNode> fetchRemainderList() {
        return fetchData(remndrPath);
    }

    private List<JsonNode> fetchData(String path) {
        if (!isAvailable()) {
            return Collections.emptyList();
        }

        // Spring RestClient의 queryParam은 `+`/`/`/`=` 등 reserved 문자를 인코딩하지 않아
        // data.go.kr 측에서 인증키가 깨져 "등록되지 않은 인증키" 에러가 발생한다.
        // URI를 직접 인코딩해 raw로 전달.
        String since = LocalDate.now().minusDays(LOOKBACK_DAYS).toString();
        String query = "serviceKey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&page=1"
                + "&perPage=" + PER_PAGE
                + "&returnType=JSON"
                + "&" + URLEncoder.encode("cond[RCRIT_PBLANC_DE::GTE]", StandardCharsets.UTF_8) + "=" + since;
        URI uri = URI.create(baseUrl + path + "?" + query);

        try {
            JsonNode response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                return Collections.emptyList();
            }

            JsonNode data = response.path("data");
            if (!data.isArray()) {
                return Collections.emptyList();
            }

            List<JsonNode> items = new ArrayList<>(data.size());
            data.forEach(items::add);
            return items;
        } catch (Exception e) {
            log.warn("청약홈 API 호출 실패 path={} : {}", path, e.getMessage());
            return Collections.emptyList();
        }
    }
}
