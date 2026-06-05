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
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LhSubscriptionServiceTest {

    private static final DateTimeFormatter LH_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

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
    void 임대유형_전체_노출_매입_전세임대도_포함() {
        when(client.isAvailable()).thenReturn(true);
        when(client.fetchAll()).thenReturn(List.of(
                rentRow("R1", "서울 강동 행복주택", "행복주택", "서울특별시"),
                rentRow("R2", "서울 노원 국민임대", "국민임대", "서울특별시"),
                rentRow("R3", "서울 공공임대 공고", "공공임대", "서울특별시"),
                rentRow("R4", "서울 매입임대 공고", "매입임대", "서울특별시"),
                rentRow("R5", "서울 전세임대 공고", "전세임대", "서울특별시")
        ));

        LhNoticesResponse res = service.findActiveToday();

        assertThat(res.rent()).extracting(LhNoticeItem::supplyTypeName)
                .containsExactlyInAnyOrder("행복주택", "국민임대", "공공임대", "매입임대", "전세임대");
    }

    @Test
    void 지역_필터_서울과_하남은_통과_부산_대구는_제외() {
        when(client.isAvailable()).thenReturn(true);
        when(client.fetchAll()).thenReturn(List.of(
                saleRow("S1", "○○지구 공공분양", "서울특별시"),
                saleRow("S2", "하남 교산 공공분양", "경기도"),
                saleRow("S3", "부산 명지 공공분양", "부산광역시"),
                saleRow("S4", "대구 ○○ 공공분양", "대구광역시")
        ));

        LhNoticesResponse res = service.findActiveToday();

        assertThat(res.sale()).extracting(LhNoticeItem::name)
                .containsExactlyInAnyOrder("○○지구 공공분양", "하남 교산 공공분양");
    }

    @Test
    void 접수마감_상태이거나_마감일_지난_공고는_제외된다() {
        LocalDate today = LocalDate.now();
        ObjectNode closedByStatus = saleRow("C1", "서울 마감상태 공공분양", "서울특별시");
        closedByStatus.put("PAN_SS", "접수마감");
        ObjectNode closedByDate = saleRow("C2", "서울 마감일지남 공공분양", "서울특별시");
        closedByDate.put("CLSG_DT", today.minusDays(1).format(LH_FMT));
        ObjectNode open = saleRow("C3", "서울 진행중 공공분양", "서울특별시");
        open.put("CLSG_DT", today.plusDays(3).format(LH_FMT));

        when(client.isAvailable()).thenReturn(true);
        when(client.fetchAll()).thenReturn(List.<JsonNode>of(closedByStatus, closedByDate, open));

        LhNoticesResponse res = service.findActiveToday();

        assertThat(res.sale()).extracting(LhNoticeItem::name)
                .containsExactly("서울 진행중 공공분양");
    }

    @Test
    void 신혼희망타운은_분양그룹_임대는_임대그룹으로_분리되고_중복_PAN_ID는_한번만() {
        when(client.isAvailable()).thenReturn(true);
        ObjectNode dup1 = saleRow("DUP", "서울 공공분양", "서울특별시");
        ObjectNode dup2 = saleRow("DUP", "서울 공공분양", "서울특별시"); // 동일 PAN_ID
        ObjectNode honeymoon = mapper.createObjectNode();
        honeymoon.put("PAN_ID", "HM");
        honeymoon.put("PAN_NM", "서울 신혼희망타운");
        honeymoon.put("AIS_TP_CD_NM", "신혼희망타운");
        honeymoon.put("CNP_CD_NM", "서울특별시");
        honeymoon.put("UPP_AIS_TP_CD", "39");
        when(client.fetchAll()).thenReturn(List.<JsonNode>of(dup1, dup2, honeymoon, rentRow("RH", "서울 행복주택", "행복주택", "서울특별시")));

        LhNoticesResponse res = service.findActiveToday();

        assertThat(res.sale()).extracting(LhNoticeItem::name)
                .containsExactlyInAnyOrder("서울 공공분양", "서울 신혼희망타운");
        assertThat(res.sale()).allMatch(i -> i.category() == LhSupplyCategory.SALE);
        assertThat(res.rent()).hasSize(1);
        assertThat(res.rent().get(0).category()).isEqualTo(LhSupplyCategory.RENT);
    }

    private ObjectNode saleRow(String panId, String name, String region) {
        ObjectNode n = mapper.createObjectNode();
        n.put("PAN_ID", panId);
        n.put("PAN_NM", name);
        n.put("AIS_TP_CD_NM", "분양주택");
        n.put("UPP_AIS_TP_NM", "분양주택");
        n.put("CNP_CD_NM", region);
        n.put("UPP_AIS_TP_CD", "05");
        n.put("PAN_SS", "접수중");
        return n;
    }

    private ObjectNode rentRow(String panId, String name, String typeName, String region) {
        ObjectNode n = mapper.createObjectNode();
        n.put("PAN_ID", panId);
        n.put("PAN_NM", name);
        n.put("AIS_TP_CD_NM", typeName);
        n.put("UPP_AIS_TP_NM", "임대주택");
        n.put("CNP_CD_NM", region);
        n.put("UPP_AIS_TP_CD", "06");
        n.put("PAN_SS", "접수중");
        return n;
    }
}
