package org.example.account.domain;

public enum CardCompany {
    HANA("하나카드"),
    SAMSUNG("삼성카드"),
    HYUNDAI("현대카드"),
    SHINHAN("신한카드"),
    KB("KB국민카드");

    private final String displayName;

    CardCompany(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
