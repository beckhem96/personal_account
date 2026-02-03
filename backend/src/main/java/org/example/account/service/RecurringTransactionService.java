package org.example.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.account.domain.Card;
import org.example.account.domain.Category;
import org.example.account.domain.PaymentMethod;
import org.example.account.domain.RecurringTransaction;
import org.example.account.domain.Transaction;
import org.example.account.dto.RecurringTransactionRequest;
import org.example.account.dto.RecurringTransactionResponse;
import org.example.account.repository.CardRepository;
import org.example.account.repository.CategoryRepository;
import org.example.account.repository.RecurringTransactionRepository;
import org.example.account.repository.TransactionRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurringTransactionService {

    private final RecurringTransactionRepository repository;
    private final CategoryRepository categoryRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public RecurringTransactionResponse create(RecurringTransactionRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Card card = null;
        if (request.paymentMethod() == PaymentMethod.CARD && request.cardId() != null) {
            card = cardRepository.findById(request.cardId())
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        }

        RecurringTransaction rt = new RecurringTransaction(
                request.name(),
                request.amount(),
                request.dayOfMonth(),
                request.paymentMethod(),
                card,
                category
        );

        RecurringTransaction saved = repository.save(rt);

        // 고정 비용 생성 시 현재 월의 Transaction도 자동 생성
        createTransactionForMonth(saved, YearMonth.now());

        return RecurringTransactionResponse.from(saved);
    }

    /**
     * 매월 1일 자정에 실행 — 등록된 모든 고정 비용에 대해 해당 월의 거래를 자동 생성
     */
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void generateMonthlyTransactions() {
        YearMonth currentMonth = YearMonth.now();
        log.info("고정 비용 월별 자동 생성 시작: {}", currentMonth);

        List<RecurringTransaction> allRecurring = repository.findAll();
        int created = 0;

        for (RecurringTransaction rt : allRecurring) {
            if (createTransactionForMonth(rt, currentMonth)) {
                created++;
            }
        }

        log.info("고정 비용 월별 자동 생성 완료: {}건 생성", created);
    }

    /**
     * 앱 시작 시 현재 월의 미생성 고정 비용 거래를 자동 생성
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void generateTransactionsOnStartup() {
        YearMonth currentMonth = YearMonth.now();
        log.info("앱 시작 시 고정 비용 거래 확인: {}", currentMonth);

        List<RecurringTransaction> allRecurring = repository.findAll();
        int created = 0;

        for (RecurringTransaction rt : allRecurring) {
            if (createTransactionForMonth(rt, currentMonth)) {
                created++;
            }
        }

        if (created > 0) {
            log.info("앱 시작 시 미생성 고정 비용 거래 {}건 자동 생성 완료", created);
        }
    }

    /**
     * 특정 월에 대한 고정 비용 거래를 생성한다.
     * 이미 해당 월에 거래가 존재하면 중복 생성하지 않는다.
     *
     * @return 거래가 생성되었으면 true, 이미 존재하여 스킵했으면 false
     */
    private boolean createTransactionForMonth(RecurringTransaction rt, YearMonth yearMonth) {
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // 중복 체크: 해당 월에 이미 이 고정 비용으로 생성된 거래가 있는지 확인
        if (transactionRepository.existsByRecurringTransactionIdAndDateBetween(
                rt.getId(), monthStart, monthEnd)) {
            return false;
        }

        LocalDate today = LocalDate.now();

        // 해당 월의 마지막 날보다 dayOfMonth가 크면 마지막 날로 설정
        int dayOfMonth = Math.min(rt.getDayOfMonth(), yearMonth.lengthOfMonth());
        LocalDate transactionDate = yearMonth.atDay(dayOfMonth);

        // 이미 지난 날짜인 경우 isConfirmed = true, 아직 안 지난 경우 false (예정)
        boolean isConfirmed = !transactionDate.isAfter(today);

        Transaction transaction = new Transaction(
                transactionDate,
                rt.getAmount(),
                rt.getName() + " (고정비용)",
                rt.getPaymentMethod(),
                rt.getCategory(),
                isConfirmed,
                rt.getCard()
        );
        transaction.associateRecurringTransaction(rt);

        transactionRepository.save(transaction);
        return true;
    }

    public List<RecurringTransactionResponse> getAll() {
        return repository.findAll().stream()
                .map(RecurringTransactionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
