package org.example.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StatementImportResponse(
        int imported,
        int skipped,
        int failed,
        int unclassified,
        List<ImportedItem> summary
) {
    public record ImportedItem(
            LocalDate date,
            String merchant,
            BigDecimal amount,
            String categoryName,
            Integer installmentSeq,
            Integer installmentMonths
    ) {
    }
}
