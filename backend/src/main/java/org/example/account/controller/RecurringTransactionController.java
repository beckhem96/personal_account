package org.example.account.controller;

import lombok.RequiredArgsConstructor;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
