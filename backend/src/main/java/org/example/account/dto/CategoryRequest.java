package org.example.account.dto;

import org.example.account.domain.TransactionType;

public record CategoryRequest(String name, TransactionType type) {
}
