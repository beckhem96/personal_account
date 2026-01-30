package org.example.account.controller;

import lombok.RequiredArgsConstructor;
import org.example.account.dto.TaxStockRequest;
import org.example.account.dto.TaxStockResponse;
import org.example.account.dto.YearEndSettlementRequest;
import org.example.account.dto.YearEndSettlementResponse;
import org.example.account.service.TaxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tax")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;

    @PostMapping("/stock")
    public ResponseEntity<TaxStockResponse> calculateStockTax(@RequestBody TaxStockRequest request) {
        return ResponseEntity.ok(taxService.calculateStockTax(request));
    }

    @PostMapping("/year-end")
    public ResponseEntity<YearEndSettlementResponse> simulateYearEnd(@RequestBody YearEndSettlementRequest request) {
        return ResponseEntity.ok(taxService.simulateYearEndSettlement(request));
    }
}
