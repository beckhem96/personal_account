package org.example.account.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum YearEndCategory {
    NONE,                // 분류 없음
    TRADITIONAL_MARKET,  // 전통시장
    PUBLIC_TRANSPORT;    // 대중교통

    @JsonCreator
    public static YearEndCategory from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NONE;
        }
        try {
            return YearEndCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
