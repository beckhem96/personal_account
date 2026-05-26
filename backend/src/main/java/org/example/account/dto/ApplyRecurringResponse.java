package org.example.account.dto;

public record ApplyRecurringResponse(
        int appliedCount,
        int deletedCount
) {
}
