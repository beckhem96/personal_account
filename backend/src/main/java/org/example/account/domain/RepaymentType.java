package org.example.account.domain;

public enum RepaymentType {
    PRINCIPAL_INTEREST, // 원리금균등분할상환
    PRINCIPAL_ONLY,     // 원금균등분할상환
    BULLET              // 만기일시상환 (이자만 매월 납부, 만기 원금 일시 상환)
}
