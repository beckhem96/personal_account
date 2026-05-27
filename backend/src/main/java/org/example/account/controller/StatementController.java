package org.example.account.controller;

import lombok.RequiredArgsConstructor;
import org.example.account.dto.StatementImportResponse;
import org.example.account.dto.SupportedCardCompany;
import org.example.account.service.StatementImportService;
import org.example.account.statement.CardStatementParserRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
public class StatementController {

    private final StatementImportService statementImportService;
    private final CardStatementParserRegistry parserRegistry;

    @GetMapping("/supported")
    public ResponseEntity<List<SupportedCardCompany>> getSupportedCompanies() {
        List<SupportedCardCompany> result = parserRegistry.supportedCompanies().stream()
                .map(SupportedCardCompany::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/import")
    public ResponseEntity<StatementImportResponse> importStatement(
            @RequestParam("cardId") Long cardId,
            @RequestPart("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        return ResponseEntity.ok(statementImportService.importStatement(cardId, file));
    }
}
