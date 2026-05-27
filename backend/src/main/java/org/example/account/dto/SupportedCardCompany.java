package org.example.account.dto;

import org.example.account.domain.CardCompany;

public record SupportedCardCompany(String code, String displayName) {
    public static SupportedCardCompany from(CardCompany company) {
        return new SupportedCardCompany(company.name(), company.getDisplayName());
    }
}
