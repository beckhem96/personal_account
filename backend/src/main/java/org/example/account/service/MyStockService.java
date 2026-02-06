package org.example.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.account.client.AlphaVantageClient;
import org.example.account.domain.MyStock;
import org.example.account.dto.MyStockRequest;
import org.example.account.dto.MyStockResponse;
import org.example.account.dto.SymbolSearchResponse;
import org.example.account.repository.MyStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyStockService {

    private final MyStockRepository myStockRepository;
    private final AlphaVantageClient alphaVantageClient;

    public List<MyStockResponse> getAllStocks() {
        return myStockRepository.findAll().stream()
                .map(MyStockResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MyStockResponse addStock(MyStockRequest request) {
        if (myStockRepository.existsByTicker(request.ticker().toUpperCase())) {
            throw new IllegalArgumentException("이미 등록된 종목입니다: " + request.ticker());
        }

        MyStock stock = new MyStock(
                request.ticker(),
                request.companyName(),
                request.purchasePrice(),
                request.quantity()
        );
        return MyStockResponse.from(myStockRepository.save(stock));
    }

    @Transactional
    public MyStockResponse updateStock(Long id, MyStockRequest request) {
        MyStock stock = myStockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다"));
        stock.updateHolding(request.purchasePrice(), request.quantity());
        return MyStockResponse.from(stock);
    }

    @Transactional
    public void deleteStock(Long id) {
        MyStock stock = myStockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다"));
        myStockRepository.delete(stock);
    }

    public List<SymbolSearchResponse> searchSymbol(String keywords) {
        return alphaVantageClient.symbolSearch(keywords);
    }

    @Transactional
    public MyStockResponse syncPrice(Long id) {
        MyStock stock = myStockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다"));

        BigDecimal price = alphaVantageClient.getGlobalQuote(stock.getTicker());
        stock.syncPrice(price);
        return MyStockResponse.from(stock);
    }

    @Transactional
    public List<MyStockResponse> syncAllPrices() {
        List<MyStock> stocks = myStockRepository.findAll();
        for (MyStock stock : stocks) {
            try {
                BigDecimal price = alphaVantageClient.getGlobalQuote(stock.getTicker());
                stock.syncPrice(price);
            } catch (Exception e) {
                log.warn("가격 동기화 실패: {} - {}", stock.getTicker(), e.getMessage());
            }
        }
        return stocks.stream()
                .map(MyStockResponse::from)
                .collect(Collectors.toList());
    }
}
