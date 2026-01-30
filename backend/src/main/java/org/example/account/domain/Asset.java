package org.example.account.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal balance; // 현재 잔액 또는 평가금액

    // For Stocks
    private BigDecimal purchasePrice; // 매수 금액 (수익률 계산용)

    public Asset(AssetType type, String name, BigDecimal balance, BigDecimal purchasePrice) {
        this.type = type;
        this.name = name;
        this.balance = balance;
        this.purchasePrice = purchasePrice;
    }

    public void update(String name, BigDecimal balance, BigDecimal purchasePrice) {
        this.name = name;
        this.balance = balance;
        this.purchasePrice = purchasePrice;
    }
}
