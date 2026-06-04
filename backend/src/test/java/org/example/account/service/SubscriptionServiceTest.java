package org.example.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.account.client.ApplyhomeClient;
import org.example.account.domain.SubscriptionRank;
import org.example.account.dto.SubscriptionItem;
import org.example.account.dto.SubscriptionsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscriptionServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private ApplyhomeClient client;
    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        client = mock(ApplyhomeClient.class);
        service = new SubscriptionService(client);
    }

    @Test
    void 키_미설정이면_빈응답_apiKeyConfigured_false() {
        when(client.isAvailable()).thenReturn(false);

        SubscriptionsResponse res = service.findActiveToday();

        assertThat(res.apiKeyConfigured()).isFalse();
        assertThat(res.firstRank()).isEmpty();
        assertThat(res.secondRank()).isEmpty();
        assertThat(res.remainder()).isEmpty();
    }

    @Test
    void 지역_필터_서울과_경기_5개시군구만_통과() {
        SubscriptionItem seoul = item("서울단지", "서울특별시", "서울특별시 강남구", List.of(SubscriptionRank.FIRST));
        SubscriptionItem namyangju = item("남양주단지", "경기", "경기도 남양주시 별내동", List.of(SubscriptionRank.FIRST));
        SubscriptionItem busan = item("부산단지", "부산광역시", "부산광역시 해운대구", List.of(SubscriptionRank.FIRST));
        SubscriptionItem suwon = item("수원단지", "경기", "경기도 수원시 영통구", List.of(SubscriptionRank.FIRST));

        assertThat(service.matchesTargetRegion(seoul)).isTrue();
        assertThat(service.matchesTargetRegion(namyangju)).isTrue();
        assertThat(service.matchesTargetRegion(busan)).isFalse();
        assertThat(service.matchesTargetRegion(suwon)).isFalse();
    }

    @Test
    void 오늘이_1순위_해당지역_접수기간_안이면_FIRST_활성() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        ObjectNode raw = mapper.createObjectNode();
        raw.put("HOUSE_MANAGE_NO", "12345");
        raw.put("HOUSE_NM", "테스트단지");
        raw.put("SUBSCRPT_AREA_CODE_NM", "서울특별시");
        raw.put("HSSPLY_ADRES", "서울특별시 강남구 역삼동");
        raw.put("GNRL_RNK1_CRSPAREA_RCPTDE", "2026-05-30");
        raw.put("GNRL_RNK1_CRSPAREA_ENDDE", "2026-06-03");

        SubscriptionItem item = service.toItemFromApt(raw, today);

        assertThat(item).isNotNull();
        assertThat(item.activeStages()).containsExactly(SubscriptionRank.FIRST);
        assertThat(item.applyhomeUrl()).contains("12345");
    }

    @Test
    void 거주지영역_달라도_기타지역_접수일이_오늘이면_FIRST_활성() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        ObjectNode raw = mapper.createObjectNode();
        raw.put("HOUSE_NM", "지방단지");
        raw.put("HSSPLY_ADRES", "서울특별시 강남구");
        // 해당지역 접수는 어제 끝났지만, 기타지역(타지방 거주자) 접수가 오늘 진행
        raw.put("GNRL_RNK1_CRSPAREA_RCPTDE", "2026-05-30");
        raw.put("GNRL_RNK1_CRSPAREA_ENDDE", "2026-05-31");
        raw.put("GNRL_RNK1_ETC_AREA_RCPTDE", "2026-06-01");
        raw.put("GNRL_RNK1_ETC_AREA_ENDDE", "2026-06-01");

        SubscriptionItem item = service.toItemFromApt(raw, today);

        assertThat(item.activeStages()).containsExactly(SubscriptionRank.FIRST);
    }

    @Test
    void 마감일이_지난_공고는_빈_단계() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        ObjectNode raw = mapper.createObjectNode();
        raw.put("HOUSE_NM", "테스트단지");
        raw.put("HSSPLY_ADRES", "서울특별시 강남구");
        raw.put("GNRL_RNK1_CRSPAREA_RCPTDE", "2026-04-01");
        raw.put("GNRL_RNK1_CRSPAREA_ENDDE", "2026-04-05");
        raw.put("GNRL_RNK2_CRSPAREA_RCPTDE", "2026-04-06");
        raw.put("GNRL_RNK2_CRSPAREA_ENDDE", "2026-04-06");

        SubscriptionItem item = service.toItemFromApt(raw, today);

        assertThat(item.activeStages()).isEmpty();
    }

    @Test
    void 접수_시작_전_예정_공고도_단계_활성() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        ObjectNode raw = mapper.createObjectNode();
        raw.put("HOUSE_NM", "예정단지");
        raw.put("HSSPLY_ADRES", "서울특별시 강남구");
        // 1순위 접수가 아직 시작 전(미래)
        raw.put("GNRL_RNK1_CRSPAREA_RCPTDE", "2026-06-10");
        raw.put("GNRL_RNK1_CRSPAREA_ENDDE", "2026-06-12");

        SubscriptionItem item = service.toItemFromApt(raw, today);

        assertThat(item.activeStages()).containsExactly(SubscriptionRank.FIRST);
    }

    @Test
    void 일반과_무순위_API를_모두_호출하고_그룹별로_분리() {
        LocalDate today = LocalDate.now();

        ObjectNode apt = mapper.createObjectNode();
        apt.put("HOUSE_MANAGE_NO", "1");
        apt.put("HOUSE_NM", "일반청약단지");
        apt.put("SUBSCRPT_AREA_CODE_NM", "서울특별시");
        apt.put("HSSPLY_ADRES", "서울특별시 강남구");
        apt.put("GNRL_RNK1_CRSPAREA_RCPTDE", today.minusDays(1).toString());
        apt.put("GNRL_RNK1_CRSPAREA_ENDDE", today.plusDays(1).toString());

        ObjectNode remndr = mapper.createObjectNode();
        remndr.put("HOUSE_MANAGE_NO", "2");
        remndr.put("HOUSE_NM", "무순위단지");
        remndr.put("HSSPLY_ADRES", "경기도 하남시 미사동");
        remndr.put("SUBSCRPT_RCEPT_BGNDE", today.minusDays(2).toString());
        remndr.put("SUBSCRPT_RCEPT_ENDDE", today.plusDays(2).toString());

        when(client.isAvailable()).thenReturn(true);
        when(client.fetchAptList()).thenReturn(List.<JsonNode>of(apt));
        when(client.fetchRemainderList()).thenReturn(List.<JsonNode>of(remndr));

        SubscriptionsResponse res = service.findActiveToday();

        assertThat(res.apiKeyConfigured()).isTrue();
        assertThat(res.firstRank()).extracting(SubscriptionItem::name).containsExactly("일반청약단지");
        assertThat(res.secondRank()).isEmpty();
        assertThat(res.remainder()).extracting(SubscriptionItem::name).containsExactly("무순위단지");
    }

    private SubscriptionItem item(String name, String region, String addr, List<SubscriptionRank> active) {
        return new SubscriptionItem(null, name, "민영", region, addr, null, null,
                null, null, null, null, null, null, active, null);
    }
}
