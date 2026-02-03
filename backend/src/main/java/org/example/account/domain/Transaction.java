package org.example.account.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private BigDecimal amount;

    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card; // Optional: Only if PaymentMethod is CARD

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_transaction_id")
    private RecurringTransaction recurringTransaction;

    @Column(nullable = false)
    private boolean isConfirmed;

    public Transaction(LocalDate date, BigDecimal amount, String memo, PaymentMethod paymentMethod, Category category, boolean isConfirmed, Card card) {
        this.date = date;
        this.amount = amount;
        this.memo = memo;
        this.paymentMethod = paymentMethod;
        this.category = category;
        this.isConfirmed = isConfirmed;
        this.card = card;
    }

    // Constructor for backward compatibility (card is null)
    public Transaction(LocalDate date, BigDecimal amount, String memo, PaymentMethod paymentMethod, Category category, boolean isConfirmed) {
        this(date, amount, memo, paymentMethod, category, isConfirmed, null);
    }

    public void confirm() {
        this.isConfirmed = true;
    }

    public void associateRecurringTransaction(RecurringTransaction recurringTransaction) {
        this.recurringTransaction = recurringTransaction;
    }

    public void update(LocalDate date, BigDecimal amount, String memo, PaymentMethod paymentMethod, Category category, Card card) {
        this.date = date;
        this.amount = amount;
        this.memo = memo;
        this.paymentMethod = paymentMethod;
        this.category = category;
        this.card = card;
    }
}