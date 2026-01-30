package org.example.account.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 월세, 통신비 등

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer dayOfMonth; // 매월 며칠

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card; // 카드 결제인 경우

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public RecurringTransaction(String name, BigDecimal amount, Integer dayOfMonth, PaymentMethod paymentMethod, Card card, Category category) {
        this.name = name;
        this.amount = amount;
        this.dayOfMonth = dayOfMonth;
        this.paymentMethod = paymentMethod;
        this.card = card;
        this.category = category;
    }

    public void update(String name, BigDecimal amount, Integer dayOfMonth, PaymentMethod paymentMethod, Card card, Category category) {
        this.name = name;
        this.amount = amount;
        this.dayOfMonth = dayOfMonth;
        this.paymentMethod = paymentMethod;
        this.card = card;
        this.category = category;
    }
}
