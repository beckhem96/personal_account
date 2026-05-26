package org.example.account.controller;

import lombok.RequiredArgsConstructor;
import org.example.account.dto.TransactionRequest;
import org.example.account.dto.TransactionResponse;
import org.example.account.service.TransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.createTransaction(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(@PathVariable Long id, @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.updateTransaction(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<TransactionResponse> confirmTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.confirmTransaction(id));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) org.example.account.domain.PaymentMethod paymentMethod
    ) {
        if (paymentMethod != null) {
            return ResponseEntity.ok(transactionService.getTransactionsByPaymentMethod(paymentMethod, startDate, endDate));
        }
        return ResponseEntity.ok(transactionService.getTransactions(startDate, endDate));
    }
    
    @GetMapping("/planned")
    public ResponseEntity<List<TransactionResponse>> getPlannedTransactions() {
        return ResponseEntity.ok(transactionService.getFuturePlannedTransactions());
    }

    @GetMapping("/by-card/{cardId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByCard(
            @PathVariable Long cardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(transactionService.getTransactionsByCard(cardId, startDate, endDate));
        }
        return ResponseEntity.ok(transactionService.getTransactionsByCard(cardId));
    }
}