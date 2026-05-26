package org.example.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.account.domain.*;
import org.example.account.dto.ApplyRecurringResponse;
import org.example.account.dto.RecurringTransactionRequest;
import org.example.account.dto.RecurringTransactionResponse;
import org.example.account.repository.AssetRepository;
import org.example.account.repository.CardRepository;
import org.example.account.repository.CategoryRepository;
import org.example.account.repository.RecurringTransactionRepository;
import org.example.account.repository.TransactionRepository;
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
    private final AssetRepository assetRepository;

    @Transactional
    public RecurringTransactionResponse create(RecurringTransactionRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Card card = null;
        if (request.paymentMethod() == PaymentMethod.CARD && request.cardId() != null) {
            card = cardRepository.findById(request.cardId())
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        }

        Asset asset = null;
        if (request.assetId() != null) {
            asset = assetRepository.findById(request.assetId())
                    .orElseThrow(() -> new IllegalArgumentException("Asset not found"));
        }

        Asset toAsset = null;
        if (request.toAssetId() != null) {
            toAsset = assetRepository.findById(request.toAssetId())
                    .orElseThrow(() -> new IllegalArgumentException("ToAsset not found"));
        }

        RecurringTransaction rt = new RecurringTransaction(
                request.name(),
                request.amount(),
                request.dayOfMonth(),
                request.paymentMethod(),
                card,
                category,
                asset,
                toAsset,
                request.startDate(),
                request.endDate()
        );

        RecurringTransaction saved = repository.save(rt);

        // 고정 비용 생성 시 Transaction 자동 생성 제거 — 일괄 적용 버튼으로만 생성

        return RecurringTransactionResponse.from(saved);
    }

    /**
     * 단일 고정 비용 적용: 특정 고정 비용의 현재 월 거래를 생성한다.
     */
    @Transactional
    public ApplyRecurringResponse applySingleRecurringTransaction(Long id) {
        RecurringTransaction rt = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RecurringTransaction not found"));

        YearMonth currentMonth = YearMonth.now();
        LocalDate today = LocalDate.now();

        // 1. 기간 만료 체크 → 삭제
        if (rt.getEndDate() != null && rt.getEndDate().isBefore(today)) {
            repository.delete(rt);
            log.info("고정 비용 기간 만료로 삭제: {}", rt.getName());
            return new ApplyRecurringResponse(0, 1);
        }

        // 2. 시작일 이전이면 적용 불가
        if (rt.getStartDate() != null && rt.getStartDate().isAfter(today)) {
            throw new IllegalArgumentException("시작일 이전이므로 적용할 수 없습니다: " + rt.getStartDate());
        }

        // 3. 해당 월 Transaction 생성
        if (createTransactionForMonth(rt, currentMonth)) {
            log.info("고정 비용 개별 적용 완료: {}", rt.getName());
            return new ApplyRecurringResponse(1, 0);
        } else {
            throw new IllegalArgumentException("이번 달에 이미 적용된 고정 비용입니다.");
        }
    }

    /**
     * 수동 일괄 적용: 현재 월의 고정 비용 거래를 생성하고 기간 만료된 항목을 삭제한다.
     */
    @Transactional
    public ApplyRecurringResponse applyAllRecurringTransactions() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> allRecurring = repository.findAll();
        int appliedCount = 0;
        int deletedCount = 0;

        for (RecurringTransaction rt : allRecurring) {
            // 1. 기간 만료 체크 → 삭제
            if (rt.getEndDate() != null && rt.getEndDate().isBefore(today)) {
                repository.delete(rt);
                deletedCount++;
                continue;
            }

            // 2. 시작일 이전이면 스킵
            if (rt.getStartDate() != null && rt.getStartDate().isAfter(today)) {
                continue;
            }

            // 3. 해당 월 Transaction 생성 (기본 자산 할당)
            if (createTransactionForMonth(rt, currentMonth)) {
                appliedCount++;
            }
        }

        log.info("고정 비용 일괄 적용 완료: {}건 생성, {}건 삭제", appliedCount, deletedCount);
        return new ApplyRecurringResponse(appliedCount, deletedCount);
    }

    /**
     * 특정 월에 대한 고정 비용 거래를 생성한다.
     * 이미 해당 월에 거래가 존재하면 중복 생성하지 않는다.
     * 자산이 지정되어 있으면 사용하고, 없으면 기본 자산을 할당한다.
     * 확정된 거래는 자산 잔액에 반영한다.
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

        // 자산 결정: 지정된 자산이 있으면 사용, 없으면 기본 자산
        Asset asset = rt.getAsset();
        Asset toAsset = rt.getToAsset();
        if (asset == null) {
            asset = assetRepository.findByIsDefaultTrue().orElse(null);
        }

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
        transaction.associateAsset(asset, toAsset);

        // 확정된 거래는 자산 잔액에 반영
        if (isConfirmed) {
            TransactionType categoryType = rt.getCategory().getType();
            String categoryName = rt.getCategory().getName();
            boolean isAssetTransfer = categoryType == TransactionType.TRANSFER || "저축/투자".equals(categoryName);

            if (isAssetTransfer && asset != null && toAsset != null) {
                // 이체/저축/투자: asset에서 출금, toAsset에 입금
                asset.subtractBalance(rt.getAmount());
                toAsset.addBalance(rt.getAmount());
            } else if (asset != null) {
                if (categoryType == TransactionType.EXPENSE) {
                    asset.subtractBalance(rt.getAmount());
                } else if (categoryType == TransactionType.INCOME) {
                    asset.addBalance(rt.getAmount());
                }
            }
        }

        transactionRepository.save(transaction);
        return true;
    }

    @Transactional
    public RecurringTransactionResponse update(Long id, RecurringTransactionRequest request) {
        RecurringTransaction rt = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RecurringTransaction not found"));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Card card = null;
        if (request.paymentMethod() == PaymentMethod.CARD && request.cardId() != null) {
            card = cardRepository.findById(request.cardId())
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        }

        Asset asset = null;
        if (request.assetId() != null) {
            asset = assetRepository.findById(request.assetId())
                    .orElseThrow(() -> new IllegalArgumentException("Asset not found"));
        }

        Asset toAsset = null;
        if (request.toAssetId() != null) {
            toAsset = assetRepository.findById(request.toAssetId())
                    .orElseThrow(() -> new IllegalArgumentException("ToAsset not found"));
        }

        rt.update(request.name(), request.amount(), request.dayOfMonth(), request.paymentMethod(), card, category, asset, toAsset, request.startDate(), request.endDate());

        // 현재 월의 미확정 거래도 함께 수정
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        List<Transaction> unconfirmedTxs = transactionRepository
                .findByRecurringTransactionIdAndIsConfirmedFalseAndDateBetween(id, monthStart, monthEnd);

        int dayOfMonth = Math.min(request.dayOfMonth(), currentMonth.lengthOfMonth());
        LocalDate newDate = currentMonth.atDay(dayOfMonth);

        for (Transaction tx : unconfirmedTxs) {
            tx.update(newDate, request.amount(), request.name() + " (고정비용)", request.paymentMethod(), category, card);
        }

        return RecurringTransactionResponse.from(rt);
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
