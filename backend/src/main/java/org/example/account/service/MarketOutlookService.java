package org.example.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.account.client.CnbcRssClient;
import org.example.account.client.GeminiClient;
import org.example.account.dto.MarketOutlookResponse;
import org.example.account.dto.RssArticle;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketOutlookService {

    private final CnbcRssClient cnbcRssClient;
    private final GeminiClient geminiClient;

    @Cacheable(value = "dailyMarketOutlook",
            key = "T(java.time.LocalDate).now(T(java.time.ZoneId).of('Asia/Seoul')).toString()")
    public MarketOutlookResponse getMarketOutlook() {
        log.info("시장 전망 리포트 생성 시작 (RSS 수집 + Gemini 분석)");

        List<RssArticle> articles = cnbcRssClient.fetchAllFeeds();

        if (articles.isEmpty()) {
            log.warn("RSS 기사가 수집되지 않았습니다");
            return new MarketOutlookResponse(
                    "현재 뉴스 피드를 수집할 수 없어 시장 전망을 생성할 수 없습니다. 잠시 후 다시 시도해주세요.",
                    LocalDateTime.now(ZoneId.of("Asia/Seoul")),
                    List.of()
            );
        }

        String prompt = buildPrompt(articles);
        String report = geminiClient.generateContent(prompt);

        log.info("시장 전망 리포트 생성 완료 (기사 {}건 기반)", articles.size());

        return new MarketOutlookResponse(
                report,
                LocalDateTime.now(ZoneId.of("Asia/Seoul")),
                articles
        );
    }

    private String buildPrompt(List<RssArticle> articles) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 월스트리트 출신의 시니어 시장 전략가야. ");
        sb.append("아래 CNBC 최신 뉴스 기사들을 분석하여 오늘의 시장 전망 리포트를 작성해줘.\n\n");
        sb.append("현재 시각: ").append(LocalDateTime.now(ZoneId.of("Asia/Seoul"))).append(" (KST)\n\n");

        sb.append("## 뉴스 기사 목록\n\n");

        for (RssArticle article : articles) {
            sb.append("### [").append(article.category()).append("] ").append(article.title()).append("\n");
            if (!article.description().isBlank()) {
                String desc = article.description();
                if (desc.length() > 300) {
                    desc = desc.substring(0, 300) + "...";
                }
                sb.append(desc).append("\n");
            }
            if (!article.pubDate().isBlank()) {
                sb.append("발행: ").append(article.pubDate()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("## 리포트 작성 지침\n\n");
        sb.append("다음 섹션을 포함한 마크다운 형식의 리포트를 작성해줘:\n\n");
        sb.append("1. 주요 이슈 요약 — 오늘 시장에 영향을 미칠 핵심 뉴스 3-5개를 요약\n");
        sb.append("2. 섹터별 전망 — 기술, 금융, 에너지, 헬스케어 등 주요 섹터의 단기 전망\n");
        sb.append("3. 시장 심리 판단 — Bullish / Bearish / Neutral 중 하나를 선택하고 근거 제시\n");
        sb.append("4. 투자 전략 제안 — 오늘 주목할 포지션 전략 (매수/매도/관망)\n");
        sb.append("5. 리스크 요인 — 주의해야 할 불확실성 요인\n\n");
        sb.append("6. 강력 매수 종목과 강력 매도 종목 — 현재 기사를 바탕으로 강력하게 매수/매도를 권장하는 각가 5가지 종목\n\n");
        sb.append("한국어로 작성해줘. 투자자에게 실질적으로 도움이 되는 구체적인 분석을 부탁해.\n");

        return sb.toString();
    }
}
