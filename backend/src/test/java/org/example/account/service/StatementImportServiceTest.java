package org.example.account.service;

import org.example.account.domain.Card;
import org.example.account.domain.CardCompany;
import org.example.account.domain.CardType;
import org.example.account.domain.Category;
import org.example.account.domain.Transaction;
import org.example.account.domain.TransactionType;
import org.example.account.statement.ParsedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatementImportServiceTest {

    private StatementImportService service;
    private Card card;
    private Category category;

    @BeforeEach
    void setUp() {
        service = new StatementImportService(null, null, null, null, null);
        card = new Card("하나카드 본인", CardType.CREDIT, CardCompany.HANA);
        category = new Category("식비", TransactionType.EXPENSE);
    }

    @Test
    void 일시불은_거래_1건() {
        ParsedTransaction p = new ParsedTransaction(
                LocalDate.of(2026, 4, 15), "스타벅스", new BigDecimal("5800"),
                null, null, null, "HANA:2026-04-15:스타벅스:5800:1"
        );

        List<Transaction> txs = service.expandInstallments(p, card, category);

        assertThat(txs).hasSize(1);
        assertThat(txs.get(0).getAmount()).isEqualByComparingTo("5800");
        assertThat(txs.get(0).getDate()).isEqualTo(LocalDate.of(2026, 4, 15));
        assertThat(txs.get(0).getExternalRef()).isEqualTo("HANA:2026-04-15:스타벅스:5800:1");
    }

    @Test
    void 할부3개월_매월_분할() {
        ParsedTransaction p = new ParsedTransaction(
                LocalDate.of(2026, 4, 20), "LG전자", new BigDecimal("900000"),
                null, 3, 1, "HANA:2026-04-20:LG전자:900000:3"
        );

        List<Transaction> txs = service.expandInstallments(p, card, category);

        assertThat(txs).hasSize(3);
        assertThat(txs.get(0).getAmount()).isEqualByComparingTo("300000");
        assertThat(txs.get(1).getAmount()).isEqualByComparingTo("300000");
        assertThat(txs.get(2).getAmount()).isEqualByComparingTo("300000");

        assertThat(txs.get(0).getDate()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(txs.get(1).getDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(txs.get(2).getDate()).isEqualTo(LocalDate.of(2026, 6, 20));

        assertThat(txs.get(0).getExternalRef()).isEqualTo("HANA:2026-04-20:LG전자:900000:1/3");
        assertThat(txs.get(2).getExternalRef()).isEqualTo("HANA:2026-04-20:LG전자:900000:3/3");

        BigDecimal sum = txs.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("900000");
    }

    @Test
    void 할부_나누어떨어지지_않는_경우_마지막_회차에_잔액_보정() {
        ParsedTransaction p = new ParsedTransaction(
                LocalDate.of(2026, 4, 20), "다이슨", new BigDecimal("1000000"),
                null, 3, 1, "HANA:2026-04-20:다이슨:1000000:3"
        );

        List<Transaction> txs = service.expandInstallments(p, card, category);

        assertThat(txs).hasSize(3);
        assertThat(txs.get(0).getAmount()).isEqualByComparingTo("333333");
        assertThat(txs.get(1).getAmount()).isEqualByComparingTo("333333");
        assertThat(txs.get(2).getAmount()).isEqualByComparingTo("333334");

        BigDecimal sum = txs.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("1000000");
    }

    @Test
    void 할부_메모에_회차_표기() {
        ParsedTransaction p = new ParsedTransaction(
                LocalDate.of(2026, 4, 20), "LG전자", new BigDecimal("900000"),
                null, 3, 1, "HANA:2026-04-20:LG전자:900000:3"
        );

        List<Transaction> txs = service.expandInstallments(p, card, category);

        assertThat(txs.get(0).getMemo()).isEqualTo("LG전자 (1/3)");
        assertThat(txs.get(1).getMemo()).isEqualTo("LG전자 (2/3)");
        assertThat(txs.get(2).getMemo()).isEqualTo("LG전자 (3/3)");
    }

    @Test
    void 신한카드인_경우_prefix가_SHINHAN_으로_동적_생성된다() {
        Card shinhanCard = new Card("신한카드 본인", CardType.CREDIT, CardCompany.SHINHAN);
        ParsedTransaction p = new ParsedTransaction(
                LocalDate.of(2026, 4, 15), "스타벅스", new BigDecimal("5800"),
                null, null, null, "SHINHAN:2026-04-15:스타벅스:5800:1"
        );

        List<Transaction> txs = service.expandInstallments(p, shinhanCard, category);

        assertThat(txs).hasSize(1);
        assertThat(txs.get(0).getExternalRef()).isEqualTo("SHINHAN:2026-04-15:스타벅스:5800:1");
    }

    @Test
    void 신한카드_할부인_경우_prefix가_SHINHAN_으로_동적_생성된다() {
        Card shinhanCard = new Card("신한카드 본인", CardType.CREDIT, CardCompany.SHINHAN);
        ParsedTransaction p = new ParsedTransaction(
                LocalDate.of(2026, 4, 20), "LG전자", new BigDecimal("900000"),
                null, 3, 1, "SHINHAN:2026-04-20:LG전자:900000:3"
        );

        List<Transaction> txs = service.expandInstallments(p, shinhanCard, category);

        assertThat(txs).hasSize(3);
        assertThat(txs.get(0).getExternalRef()).isEqualTo("SHINHAN:2026-04-20:LG전자:900000:1/3");
    }

    @SuppressWarnings("unused")
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
