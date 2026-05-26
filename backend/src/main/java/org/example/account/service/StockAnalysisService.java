package org.example.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.account.client.AlphaVantageClient;
import org.example.account.client.GeminiClient;
import org.example.account.domain.MyStock;
import org.example.account.dto.StockAnalysisResponse;
import org.example.account.repository.MyStockRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAnalysisService {

    private final MyStockRepository myStockRepository;
    private final AlphaVantageClient alphaVantageClient;
    private final GeminiClient geminiClient;

    public StockAnalysisResponse analyze(Long stockId) {
        MyStock stock = myStockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다"));

        String ticker = stock.getTicker();

        // 기술적 지표 수집
        Map<String, String> indicators = new LinkedHashMap<>();
        try {
            Map<String, String> rsi = alphaVantageClient.getRsi(ticker);
            rsi.forEach((k, v) -> indicators.put("RSI_" + k, v));
        } catch (Exception e) {
            log.warn("RSI 조회 실패: {}", e.getMessage());
        }

        try {
            Map<String, String> sma20 = alphaVantageClient.getSma(ticker, 20);
            sma20.forEach((k, v) -> indicators.put("SMA20_" + k, v));
        } catch (Exception e) {
            log.warn("SMA20 조회 실패: {}", e.getMessage());
        }

        try {
            Map<String, String> sma50 = alphaVantageClient.getSma(ticker, 50);
            sma50.forEach((k, v) -> indicators.put("SMA50_" + k, v));
        } catch (Exception e) {
            log.warn("SMA50 조회 실패: {}", e.getMessage());
        }

        try {
            Map<String, String> bbands = alphaVantageClient.getBbands(ticker);
            bbands.forEach((k, v) -> indicators.put("BBANDS_" + k, v));
        } catch (Exception e) {
            log.warn("BBANDS 조회 실패: {}", e.getMessage());
        }

        // 뉴스 수집
        List<Map<String, String>> rawNews;
        try {
            rawNews = alphaVantageClient.getNewsSentiment(ticker, 5);
        } catch (Exception e) {
            log.warn("뉴스 조회 실패: {}", e.getMessage());
            rawNews = List.of();
        }

        List<StockAnalysisResponse.NewsItem> newsItems = rawNews.stream()
                .map(n -> new StockAnalysisResponse.NewsItem(
                        n.getOrDefault("title", ""),
                        n.getOrDefault("url", ""),
                        n.getOrDefault("summary", ""),
                        n.getOrDefault("overall_sentiment_label", "")
                ))
                .collect(Collectors.toList());

        // Gemini 프롬프트 구성
        String prompt = buildAnalysisPrompt(ticker, stock, indicators, newsItems);
        String report = geminiClient.generateContent(prompt);

        return new StockAnalysisResponse(ticker, report, indicators, newsItems);
    }

    private String buildAnalysisPrompt(String ticker, MyStock stock,
                                       Map<String, String> indicators,
                                       List<StockAnalysisResponse.NewsItem> news) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 전문 퀀트 분석가야. 아래 제공된 [기술적 지표]와 [최신 뉴스]를 바탕으로 ")
                .append(ticker).append("의 단기/중기 전망을 분석해줘.\n\n");
        sb.append("최신 뉴스는 직접 검색해서 정보 찾아봐줘.");
        sb.append("현재 시각은 ").append(LocalDateTime.now());
        sb.append("반드시 **목표가(Target)**, **손절가(Stop-loss)**, **현재 대응 전략(Buy/Hold/Sell)**을 포함해줘.\n\n");

        sb.append("## 보유 정보\n");
        sb.append("- 평단가: $").append(stock.getPurchasePrice()).append("\n");
        sb.append("- 수량: ").append(stock.getQuantity()).append("주\n");
        if (stock.getCurrentPrice() != null) {
            sb.append("- 현재가: $").append(stock.getCurrentPrice()).append("\n");
        }
        sb.append("\n");

        sb.append("## 기술적 지표\n");
        indicators.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        sb.append("\n");

        sb.append("## 최신 뉴스\n");
        for (StockAnalysisResponse.NewsItem item : news) {
            sb.append("- [").append(item.sentiment()).append("] ").append(item.title()).append("\n");
            if (!item.summary().isEmpty()) {
                sb.append("  ").append(item.summary(), 0, Math.min(item.summary().length(), 200)).append("\n");
            }
        }

        sb.append("\n한국어로 분석 리포트를 마크다운 형식으로 작성해줘.");
        return sb.toString();
    }
}
