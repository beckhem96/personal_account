package org.example.account.dto;

import org.example.account.domain.TransactionType;
import org.example.account.domain.YearEndCategory;

public record CategoryRequest(String name, TransactionType type, YearEndCategory yearEndCategory) {
    public CategoryRequest {
        if (yearEndCategory == null) {
            yearEndCategory = YearEndCategory.NONE;
        }
    }
}
