package org.example.account.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "my_stock")
public class MyStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ticker;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal purchasePrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal currentPrice;

    private LocalDateTime lastSyncedAt;

    public MyStock(String ticker, String companyName, BigDecimal purchasePrice, Integer quantity) {
        this.ticker = ticker.toUpperCase();
        this.companyName = companyName;
        this.purchasePrice = purchasePrice;
        this.quantity = quantity;
    }

    public void syncPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
        this.lastSyncedAt = LocalDateTime.now();
    }

    public void updateHolding(BigDecimal purchasePrice, Integer quantity) {
        this.purchasePrice = purchasePrice;
        this.quantity = quantity;
    }
}
