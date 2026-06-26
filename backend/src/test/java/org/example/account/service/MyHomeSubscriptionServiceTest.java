package org.example.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.account.client.MyHomeClient;
import org.example.account.dto.LhNoticeItem;
import org.example.account.dto.LhNoticesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyHomeSubscriptionServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private MyHomeClient myHomeClient;
    private MyHomeSubscriptionService service;

    @BeforeEach
    void setUp() {
        myHomeClient = mock(MyHomeClient.class);
        service = new MyHomeSubscriptionService(myHomeClient);
    }

    @Test
    void SH와_GH_공고를_공급기관명으로_분류하고_타겟지역만_필터링한다() {
        LocalDate today = LocalDate.now();

        // Given
        // 1. 서울 마포 (SH) - 통과
        ObjectNode shSeoul = mapper.createObjectNode();
        shSeoul.put("pblancNm", "SH 서울 공고");
        shSeoul.put("hssplyAdres", "서울특별시 마포구 백범로");
        shSeoul.put("rcritNtcDe", today.minusDays(5).toString());
        shSeoul.put("rceptBgnde", today.minusDays(1).toString());
        shSeoul.put("rceptEndde", today.plusDays(5).toString());
        shSeoul.put("pblancUrl", "http://sh.or.kr/1");
        shSeoul.put("suplyInsttNm", "서울주택도시공사");
        shSeoul.put("uppAisTpCdNm", "임대주택");

        // 2. 경기 남양주 (GH) - 통과
        ObjectNode ghNamyangju = mapper.createObjectNode();
        ghNamyangju.put("pblancNm", "GH 남양주 공고");
        ghNamyangju.put("hssplyAdres", "경기도 남양주시 와부읍");
        ghNamyangju.put("rcritNtcDe", today.minusDays(5).toString());
        ghNamyangju.put("rceptBgnde", today.minusDays(1).toString());
        ghNamyangju.put("rceptEndde", today.plusDays(5).toString());
        ghNamyangju.put("pblancUrl", "http://gh.or.kr/2");
        ghNamyangju.put("suplyInsttNm", "경기주택도시공사");
        ghNamyangju.put("uppAisTpCdNm", "임대주택");

        // 3. 경기 부산 (GH) - 필터링 탈락 (타겟 지역 아님)
        ObjectNode ghBusan = mapper.createObjectNode();
        ghBusan.put("pblancNm", "GH 부산 공고");
        ghBusan.put("hssplyAdres", "부산광역시 해운대구");
        ghBusan.put("rcritNtcDe", today.minusDays(5).toString());
        ghBusan.put("rceptBgnde", today.minusDays(1).toString());
        ghBusan.put("rceptEndde", today.plusDays(5).toString());
        ghBusan.put("pblancUrl", "http://gh.or.kr/3");
        ghBusan.put("suplyInsttNm", "경기주택도시공사");
        ghBusan.put("uppAisTpCdNm", "임대주택");

        when(myHomeClient.isAvailable()).thenReturn(true);
        when(myHomeClient.fetchAll()).thenReturn(List.<JsonNode>of(shSeoul, ghNamyangju, ghBusan));

        // When
        LhNoticesResponse shResponse = service.findShActiveToday();
        LhNoticesResponse ghResponse = service.findGhActiveToday();

        // Then
        assertThat(shResponse.apiKeyConfigured()).isTrue();
        assertThat(shResponse.rent()).extracting(LhNoticeItem::name).containsExactly("SH 서울 공고");

        assertThat(ghResponse.apiKeyConfigured()).isTrue();
        assertThat(ghResponse.rent()).extracting(LhNoticeItem::name).containsExactly("GH 남양주 공고");
    }
}
