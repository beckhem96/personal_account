package org.example.account.service;

import lombok.RequiredArgsConstructor;
import org.example.account.domain.*;
import org.example.account.dto.TransactionRequest;
import org.example.account.dto.TransactionResponse;
import org.example.account.repository.AssetRepository;
import org.example.account.repository.CardRepository;
import org.example.account.repository.CategoryRepository;
import org.example.account.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CardRepository cardRepository;
    private final AssetRepository assetRepository;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
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

        // INCOME/EXPENSE에서 assetId 미지정 시 기본 자산 자동 사용 (단, 저축/투자는 이체처럼 명시적 지정 필요)
        boolean isAssetTransfer = category.getType() == TransactionType.TRANSFER || "저축/투자".equals(category.getName());
        if (asset == null && !isAssetTransfer) {
            asset = assetRepository.findByIsDefaultTrue().orElse(null);
        }

        Asset toAsset = null;
        if (request.toAssetId() != null) {
            toAsset = assetRepository.findById(request.toAssetId())
                    .orElseThrow(() -> new IllegalArgumentException("To Asset not found"));
        }

        boolean confirmed = request.isConfirmed() != null ? request.isConfirmed() : true;

        Transaction transaction = new Transaction(
                request.date(),
                request.amount(),
                request.memo(),
                request.paymentMethod(),
                category,
                confirmed,
                card
        );
        transaction.associateAsset(asset, toAsset);

        if (confirmed) {
            applyAssetBalance(category, asset, toAsset, request.amount());
        }

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        // 기존 거래가 확정 상태였으면 자산 되돌림
        if (transaction.isConfirmed() && transaction.getAsset() != null) {
            reverseAssetBalance(
                    transaction.getCategory(),
                    transaction.getAsset(),
                    transaction.getToAsset(),
                    transaction.getAmount()
            );
        }

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

        // INCOME/EXPENSE에서 assetId 미지정 시 기본 자산 자동 사용 (단, 저축/투자는 이체처럼 명시적 지정 필요)
        boolean isAssetTransfer = category.getType() == TransactionType.TRANSFER || "저축/투자".equals(category.getName());
        if (asset == null && !isAssetTransfer) {
            asset = assetRepository.findByIsDefaultTrue().orElse(null);
        }

        Asset toAsset = null;
        if (request.toAssetId() != null) {
            toAsset = assetRepository.findById(request.toAssetId())
                    .orElseThrow(() -> new IllegalArgumentException("To Asset not found"));
        }

        transaction.update(
                request.date(),
                request.amount(),
                request.memo(),
                request.paymentMethod(),
                category,
                card
        );
        transaction.associateAsset(asset, toAsset);

        boolean confirmed = request.isConfirmed() != null ? request.isConfirmed() : transaction.isConfirmed();

        // 새 값이 확정이면 자산 반영
        if (confirmed) {
            applyAssetBalance(category, asset, toAsset, request.amount());
        }

        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        // 확정 상태이고 자산이 연결되어 있으면 되돌림
        if (transaction.isConfirmed() && transaction.getAsset() != null) {
            reverseAssetBalance(
                    transaction.getCategory(),
                    transaction.getAsset(),
                    transaction.getToAsset(),
                    transaction.getAmount()
            );
        }

        transactionRepository.delete(transaction);
    }

    @Transactional
    public TransactionResponse confirmTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        transaction.confirm();

        // 확정 시 자산 반영
        if (transaction.getAsset() != null) {
            applyAssetBalance(
                    transaction.getCategory(),
                    transaction.getAsset(),
                    transaction.getToAsset(),
                    transaction.getAmount()
            );
        }

        return TransactionResponse.from(transaction);
    }

    public List<TransactionResponse> getTransactions(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByDateBetween(startDate, endDate).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsByPaymentMethod(PaymentMethod paymentMethod, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByPaymentMethodAndDateBetween(paymentMethod, startDate, endDate).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getFuturePlannedTransactions() {
        return transactionRepository.findFuturePlannedTransactions(LocalDate.now()).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsByCard(Long cardId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByCardIdAndDateBetween(cardId, startDate, endDate).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsByCard(Long cardId) {
        return transactionRepository.findByCardId(cardId).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 자산 잔액 반영 (확정 시)
     * INCOME → asset 잔액 증가
     * EXPENSE → asset 잔액 감소
     * TRANSFER 또는 저축/투자 → asset 잔액 감소 + toAsset 잔액 증가
     */
    private void applyAssetBalance(Category category, Asset asset, Asset toAsset, BigDecimal amount) {
        if (asset == null) return;

        boolean isAssetTransfer = category.getType() == TransactionType.TRANSFER || "저축/투자".equals(category.getName());

        if (isAssetTransfer) {
            asset.subtractBalance(amount);
            if (toAsset != null) {
                toAsset.addBalance(amount);
            }
        } else {
            switch (category.getType()) {
                case INCOME -> asset.addBalance(amount);
                case EXPENSE -> asset.subtractBalance(amount);
            }
        }
    }

    /**
     * 자산 잔액 되돌림 (삭제/수정 시)
     * INCOME → asset 잔액 감소
     * EXPENSE → asset 잔액 증가
     * TRANSFER 또는 저축/투자 → asset 잔액 증가 + toAsset 잔액 감소
     */
    private void reverseAssetBalance(Category category, Asset asset, Asset toAsset, BigDecimal amount) {
        if (asset == null) return;

        boolean isAssetTransfer = category.getType() == TransactionType.TRANSFER || "저축/투자".equals(category.getName());

        if (isAssetTransfer) {
            asset.addBalance(amount);
            if (toAsset != null) {
                toAsset.subtractBalance(amount);
            }
        } else {
            switch (category.getType()) {
                case INCOME -> asset.subtractBalance(amount);
                case EXPENSE -> asset.addBalance(amount);
            }
        }
    }
}
