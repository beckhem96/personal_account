package org.example.account.dto;

import org.example.account.domain.CardType;

public record CardRequest(String name, CardType type) {
}
