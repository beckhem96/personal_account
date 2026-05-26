package org.example.account.controller;

import lombok.RequiredArgsConstructor;
import org.example.account.dto.MarketOutlookResponse;
import org.example.account.dto.MyStockRequest;
import org.example.account.dto.MyStockResponse;
import org.example.account.dto.StockAnalysisResponse;
import org.example.account.dto.SymbolSearchResponse;
import org.example.account.service.MarketOutlookService;
import org.example.account.service.MyStockService;
import org.example.account.service.StockAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class MyStockController {

    private final MyStockService myStockService;
    private final StockAnalysisService stockAnalysisService;
    private final MarketOutlookService marketOutlookService;

    @GetMapping
    public ResponseEntity<List<MyStockResponse>> getAllStocks() {
        return ResponseEntity.ok(myStockService.getAllStocks());
    }

    @PostMapping
    public ResponseEntity<MyStockResponse> addStock(@RequestBody MyStockRequest request) {
        return ResponseEntity.ok(myStockService.addStock(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MyStockResponse> updateStock(@PathVariable Long id, @RequestBody MyStockRequest request) {
        return ResponseEntity.ok(myStockService.updateStock(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        myStockService.deleteStock(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<SymbolSearchResponse>> searchSymbol(@RequestParam String keywords) {
        return ResponseEntity.ok(myStockService.searchSymbol(keywords));
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<MyStockResponse> syncPrice(@PathVariable Long id) {
        return ResponseEntity.ok(myStockService.syncPrice(id));
    }

    @PostMapping("/sync-all")
    public ResponseEntity<List<MyStockResponse>> syncAllPrices() {
        return ResponseEntity.ok(myStockService.syncAllPrices());
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<StockAnalysisResponse> analyze(@PathVariable Long id) {
        return ResponseEntity.ok(stockAnalysisService.analyze(id));
    }

    @GetMapping("/market-outlook")
    public ResponseEntity<MarketOutlookResponse> getMarketOutlook() {
        return ResponseEntity.ok(marketOutlookService.getMarketOutlook());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
