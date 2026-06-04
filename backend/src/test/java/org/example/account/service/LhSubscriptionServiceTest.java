package org.example.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.account.client.LhClient;
import org.example.account.domain.LhSupplyCategory;
import org.example.account.dto.LhNoticeItem;
import org.example.account.dto.LhNoticesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LhSubscriptionServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private LhClient client;
    private LhSubscriptionService service;

    @BeforeEach
    void setUp() {
        client = mock(LhClient.class);
        service = new LhSubscriptionService(client);
    }

    @Test
    void 키_미설정이면_빈응답_apiKeyConfigured_false() {
        when(client.isAvailable()).thenReturn(false);

        LhNoticesResponse res = service.findActiveToday();

        assertThat(res.apiKeyConfigured()).isFalse();
        assertThat(res.sale()).isEmpty();
        assertThat(res.rent()).isEmpty();
    }

    @Test
    void 임대유형_필터_행복주택_국민임대만_통과하고_매입_전세임대는_제외() {
        when(client.isAvailable()).thenReturn(true);
        when(client.fetchSaleNotices()).thenReturn(List.of());
        when(client.fetchRentNotices()).thenReturn(List.of(
                rentRow("서울 강동 행복주택", "행복주택"),
                rentRow("서울 노원 국민임대", "국민임대"),
                rentRow("서울 매입임대 공고", "매입임대"),
                rentRow("서울 전세임대 공고", "전세임대")
        ));

        LhNoticesResponse res = service.findActiveToday();

        assertThat(res.rent()).extracting(LhNoticeItem::supplyTypeName)
                .containsExactlyInAnyOrder("행복주택", "국민임대");
    }

    @Test
    void 지역_필터_서울과_하남은_통과_부산_대구는_제외() {
        when(client.isAvailable()).thenReturn(true);
        when(client.fetchRentNotices()).thenReturn(List.of());
        when(client.fetchSaleNotices()).thenReturn(List.of(
                saleRow("○○지구 공공분양", "서울특별시"),
                saleRow("하남 교산 공공분양", "경기도"),
                saleRow("부산 명지 공공분양", "부산광역시"),
                saleRow("대구 ○○ 공공분양", "대구광역시")
        ));

        LhNoticesResponse res = service.findActiveToday();

        assertThat(res.sale()).extracting(LhNoticeItem::name)
                .containsExactlyInAnyOrder("○○지구 공공분양", "하남 교산 공공분양");
    }

    @Test
    void 접수마감이_지난_공고는_제외된다() {
        LocalDate today = LocalDate.now();
        ObjectNode past = saleRow("서울 마감된 공공분양", "서울특별시");
        past.put("CLSG_DT", today.minusDays(1).toString());
        ObjectNode open = saleRow("서울 진행중 공공분양", "서울특별시");
        open.put("CLSG_DT", today.plusDays(3).toString());

        when(client.isAvailable()).thenReturn(true);
        when(client.fetchRentNotices()).thenReturn(List.of());
        when(client.fetchSaleNotices()).thenReturn(List.<JsonNode>of(past, open));

        LhNoticesResponse res = service.findActiveToday();

        assertThat(res.sale()).extracting(LhNoticeItem::name)
                .containsExactly("서울 진행중 공공분양");
    }

    @Test
    void 분양과_임대가_각_그룹으로_분리된다() {
        when(client.isAvailable()).thenReturn(true);
        when(client.fetchSaleNotices()).thenReturn(List.<JsonNode>of(saleRow("서울 공공분양", "서울특별시")));
        when(client.fetchRentNotices()).thenReturn(List.<JsonNode>of(rentRow("서울 행복주택", "행복주택")));

        LhNoticesResponse res = service.findActiveToday();

        assertThat(res.sale()).hasSize(1);
        assertThat(res.sale().get(0).category()).isEqualTo(LhSupplyCategory.SALE);
        assertThat(res.rent()).hasSize(1);
        assertThat(res.rent().get(0).category()).isEqualTo(LhSupplyCategory.RENT);
    }

    private ObjectNode saleRow(String name, String region) {
        ObjectNode n = mapper.createObjectNode();
        n.put("PAN_ID", "100" + name.hashCode());
        n.put("PAN_NM", name);
        n.put("AIS_TP_CD_NM", "공공분양");
        n.put("CNP_CD_NM", region);
        n.put("UPP_AIS_TP_CD", "05");
        return n;
    }

    private ObjectNode rentRow(String name, String typeName) {
        ObjectNode n = mapper.createObjectNode();
        n.put("PAN_ID", "200" + name.hashCode());
        n.put("PAN_NM", name);
        n.put("AIS_TP_CD_NM", typeName);
        n.put("CNP_CD_NM", "서울특별시");
        n.put("UPP_AIS_TP_CD", "06");
        return n;
    }
}
