package org.example.account.dto;

import org.example.account.domain.Category;
import org.example.account.domain.TransactionType;

public record CategoryResponse(Long id, String name, TransactionType type) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getType());
    }
}
