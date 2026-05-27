package org.example.account.service;

import org.example.account.domain.Card;
import org.example.account.domain.CardType;
import org.example.account.domain.Category;
import org.example.account.domain.PaymentMethod;
import org.example.account.domain.Transaction;
import org.example.account.domain.TransactionType;
import org.example.account.domain.YearEndCategory;
import org.example.account.dto.YearEndSettlementRequest;
import org.example.account.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaxServiceTest {

    private TransactionRepository transactionRepository;
    private TaxService taxService;

    private Card creditCard;
    private Card checkCard;
    private Category salary;
    private Category food;
    private Category traditionalMarket;
    private Category publicTransport;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        taxService = new TaxService(transactionRepository);

        creditCard = new Card("신용카드", CardType.CREDIT, null);
        checkCard = new Card("체크카드", CardType.CHECK, null);
        salary = new Category("월급", TransactionType.INCOME);
        food = new Category("식비", TransactionType.EXPENSE);
        traditionalMarket = new Category("재래시장", TransactionType.EXPENSE, YearEndCategory.TRADITIONAL_MARKET);
        publicTransport = new Category("대중교통", TransactionType.EXPENSE, YearEndCategory.PUBLIC_TRANSPORT);
    }

    @Test
    void 분류별_정확합산() {
        List<Transaction> txs = List.of(
                tx("2026-03-25", new BigDecimal("5000000"), PaymentMethod.BANK_TRANSFER, salary, null, true),
                tx("2026-04-15", new BigDecimal("10000"), PaymentMethod.CARD, food, creditCard, true),
                tx("2026-04-16", new BigDecimal("25000"), PaymentMethod.CARD, food, creditCard, true),
                tx("2026-04-20", new BigDecimal("8000"), PaymentMethod.CARD, food, checkCard, true),
                tx("2026-05-01", new BigDecimal("3000"), PaymentMethod.CASH, food, null, true),
                tx("2026-05-10", new BigDecimal("12000"), PaymentMethod.CARD, traditionalMarket, checkCard, true),
                tx("2026-05-12", new BigDecimal("1500"), PaymentMethod.CARD, publicTransport, checkCard, true)
        );
        when(transactionRepository.findConfirmedWithJoinsBetween(any(), any())).thenReturn(txs);

        YearEndSettlementRequest req = taxService.computeYearEndFromTransactions(2026);

        assertThat(req.totalSalary()).isEqualByComparingTo("5000000");
        assertThat(req.creditCardAmount()).isEqualByComparingTo("35000"); // 10000 + 25000
        // 체크카드(8000 + 12000 + 1500) + 현금(3000) = 24500
        assertThat(req.debitCashAmount()).isEqualByComparingTo("24500");
        assertThat(req.traditionalMarketAmount()).isEqualByComparingTo("12000");
        assertThat(req.publicTransportAmount()).isEqualByComparingTo("1500");
    }

    @Test
    void 카드없는_CARD_결제는_체크현금에_합산() {
        List<Transaction> txs = List.of(
                tx("2026-04-15", new BigDecimal("7000"), PaymentMethod.CARD, food, null, true)
        );
        when(transactionRepository.findConfirmedWithJoinsBetween(any(), any())).thenReturn(txs);

        YearEndSettlementRequest req = taxService.computeYearEndFromTransactions(2026);

        assertThat(req.creditCardAmount()).isEqualByComparingTo("0");
        assertThat(req.debitCashAmount()).isEqualByComparingTo("7000");
    }

    @Test
    void 부수입_카테고리는_총급여에_포함되지_않음() {
        Category sideIncome = new Category("부수입", TransactionType.INCOME);
        List<Transaction> txs = List.of(
                tx("2026-03-25", new BigDecimal("5000000"), PaymentMethod.BANK_TRANSFER, salary, null, true),
                tx("2026-06-01", new BigDecimal("1000000"), PaymentMethod.BANK_TRANSFER, sideIncome, null, true)
        );
        when(transactionRepository.findConfirmedWithJoinsBetween(any(), any())).thenReturn(txs);

        YearEndSettlementRequest req = taxService.computeYearEndFromTransactions(2026);

        assertThat(req.totalSalary()).isEqualByComparingTo("5000000");
    }

    @Test
    void 데이터_없으면_모두_0() {
        when(transactionRepository.findConfirmedWithJoinsBetween(any(), any())).thenReturn(List.of());

        YearEndSettlementRequest req = taxService.computeYearEndFromTransactions(2024);

        assertThat(req.totalSalary()).isEqualByComparingTo("0");
        assertThat(req.creditCardAmount()).isEqualByComparingTo("0");
        assertThat(req.debitCashAmount()).isEqualByComparingTo("0");
        assertThat(req.traditionalMarketAmount()).isEqualByComparingTo("0");
        assertThat(req.publicTransportAmount()).isEqualByComparingTo("0");
    }

    private Transaction tx(String date, BigDecimal amount, PaymentMethod pm, Category category, Card card, boolean confirmed) {
        return new Transaction(LocalDate.parse(date), amount, null, pm, category, confirmed, card);
    }
}
