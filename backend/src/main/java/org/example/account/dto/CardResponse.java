package org.example.account.dto;

import org.example.account.domain.Card;
import org.example.account.domain.CardType;

public record CardResponse(Long id, String name, CardType type) {
    public static CardResponse from(Card card) {
        return new CardResponse(card.getId(), card.getName(), card.getType());
    }
}
