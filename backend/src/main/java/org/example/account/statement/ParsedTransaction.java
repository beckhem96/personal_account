package org.example.account.statement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedTransaction(
        LocalDate date,
        String merchant,
        BigDecimal amount,
        String memo,
        Integer installmentMonths,
        Integer installmentSeq,
        String naturalKey
) {
    public boolean isInstallment() {
        return installmentMonths != null && installmentMonths > 1;
    }
}
