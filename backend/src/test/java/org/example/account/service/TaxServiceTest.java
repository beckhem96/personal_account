package org.example.account.service;

import org.example.account.domain.Card;
import org.example.account.domain.CardType;
import org.example.account.domain.Category;
import org.example.account.domain.PaymentMethod;
import org.example.account.domain.Transaction;
import org.example.account.domain.TransactionType;
import org.example.account.domain.YearEndCategory;
import org.example.account.dto.YearEndFullRequest;
import org.example.account.dto.YearEndFullResponse;
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

    @Test
    void 연말정산_정밀_총급여5천만_단계별_결정세액() {
        // 총급여 5천만, 부양가족 없음, 국민연금 225만 / 건강 197만 / 고용 45만, 나머지 0
        YearEndFullRequest req = new YearEndFullRequest(
                bd(50_000_000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0, 0, 0,
                bd(2_250_000), bd(1_970_000), bd(450_000),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        YearEndFullResponse res = taxService.calculateYearEndFull(req);

        assertThat(res.earnedIncomeDeduction()).isEqualByComparingTo("12250000");
        assertThat(res.earnedIncome()).isEqualByComparingTo("37750000");
        assertThat(res.personalDeduction()).isEqualByComparingTo("1500000");
        assertThat(res.taxBase()).isEqualByComparingTo("31580000");
        assertThat(res.calculatedTax()).isEqualByComparingTo("3477000");
        assertThat(res.earnedIncomeTaxCredit()).isEqualByComparingTo("660000");
        assertThat(res.determinedTax()).isEqualByComparingTo("2817000");
        assertThat(res.localIncomeTax()).isEqualByComparingTo("281700");
        assertThat(res.refundOrPay()).isEqualByComparingTo("-2817000"); // 기납부 0 → 전액 추가납부
    }

    @Test
    void 연말정산_정밀_기납부세액_많으면_환급() {
        YearEndFullRequest req = new YearEndFullRequest(
                bd(50_000_000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0, 0, 0,
                bd(2_250_000), bd(1_970_000), bd(450_000),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                bd(6_000_000),               // 연금저축 600만
                bd(4_000_000)                // 기납부 400만
        );

        YearEndFullResponse res = taxService.calculateYearEndFull(req);

        // 연금계좌 세액공제: 총급여 5500만 이하 → 15%, 600만 × 15% = 90만
        assertThat(res.pensionAccountCredit()).isEqualByComparingTo("900000");
        // 결정세액 2,817,000 - 900,000 = 1,917,000, 기납부 400만 → 환급 2,083,000
        assertThat(res.determinedTax()).isEqualByComparingTo("1917000");
        assertThat(res.refundOrPay()).isEqualByComparingTo("2083000");
    }

    @Test
    void 산출세액_누진구간_경계() {
        assertThat(TaxService.progressiveTax(bd(14_000_000))).isEqualByComparingTo("840000"); // 6%
        assertThat(TaxService.progressiveTax(bd(50_000_000))).isEqualByComparingTo("6240000"); // 15% - 126만
        assertThat(TaxService.progressiveTax(BigDecimal.ZERO)).isEqualByComparingTo("0");
    }

    private static BigDecimal bd(long v) {
        return BigDecimal.valueOf(v);
    }

    private Transaction tx(String date, BigDecimal amount, PaymentMethod pm, Category category, Card card, boolean confirmed) {
        return new Transaction(LocalDate.parse(date), amount, null, pm, category, confirmed, card);
    }
}
