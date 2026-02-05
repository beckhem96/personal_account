package org.example.account.controller;

import lombok.RequiredArgsConstructor;
import org.example.account.dto.ApplyRecurringResponse;
import org.example.account.dto.RecurringTransactionRequest;
import org.example.account.dto.RecurringTransactionResponse;
import org.example.account.service.RecurringTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService service;

    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> create(@RequestBody RecurringTransactionRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<RecurringTransactionResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransactionResponse> update(@PathVariable Long id, @RequestBody RecurringTransactionRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/apply")
    public ResponseEntity<ApplyRecurringResponse> applyAll() {
        return ResponseEntity.ok(service.applyAllRecurringTransactions());
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<ApplyRecurringResponse> applySingle(@PathVariable Long id) {
        return ResponseEntity.ok(service.applySingleRecurringTransaction(id));
    }
}
