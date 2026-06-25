package org.example.account.service;

import org.example.account.domain.*;
import org.example.account.dto.TransactionRequest;
import org.example.account.repository.AssetRepository;
import org.example.account.repository.CardRepository;
import org.example.account.repository.CategoryRepository;
import org.example.account.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Category transferCategory;
    private Category investmentCategory;
    private Category expenseCategory;
    private Asset sourceAsset;
    private Asset targetAsset;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        transferCategory = new Category("계좌이체", TransactionType.TRANSFER);
        investmentCategory = new Category("저축/투자", TransactionType.EXPENSE); // 이름이 '저축/투자'인 지출/이체 성격 카테고리
        expenseCategory = new Category("식비", TransactionType.EXPENSE);

        sourceAsset = new Asset(AssetType.CASH, "현금 지갑", new BigDecimal("100000"), BigDecimal.ZERO);
        targetAsset = new Asset(AssetType.SAVINGS, "정기적금", new BigDecimal("50000"), BigDecimal.ZERO);
    }

    @Test
    void 이체_카테고리_거래에서_출금자산이_누락되면_예외가_발생한다() {
        // Given
        TransactionRequest request = new TransactionRequest(
                LocalDate.now(),
                new BigDecimal("10000"),
                "이체 테스트",
                PaymentMethod.BANK_TRANSFER,
                1L, // categoryId
                true, // isConfirmed
                null, // cardId
                null, // assetId (From 누락)
                2L // toAssetId
        );

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(transferCategory));
        when(assetRepository.findById(2L)).thenReturn(Optional.of(targetAsset));

        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이체 또는 저축/투자 거래에는 출금 자산(From)을 필수 선택해야 합니다.");
    }

    @Test
    void 이체_카테고리_거래에서_입금자산이_누락되면_예외가_발생한다() {
        // Given
        TransactionRequest request = new TransactionRequest(
                LocalDate.now(),
                new BigDecimal("10000"),
                "이체 테스트",
                PaymentMethod.BANK_TRANSFER,
                1L, // categoryId
                true, // isConfirmed
                null, // cardId
                1L, // assetId
                null // toAssetId (To 누락)
        );

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(transferCategory));
        when(assetRepository.findById(1L)).thenReturn(Optional.of(sourceAsset));

        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이체 또는 저축/투자 거래에는 입금 자산(To)을 필수 선택해야 합니다.");
    }

    @Test
    void 저축투자_카테고리_거래에서_출금자산이_누락되면_예외가_발생한다() {
        // Given
        TransactionRequest request = new TransactionRequest(
                LocalDate.now(),
                new BigDecimal("50000"),
                "적금 납입",
                PaymentMethod.BANK_TRANSFER,
                2L, // categoryId
                true, // isConfirmed
                null, // cardId
                null, // assetId (From 누락)
                2L // toAssetId
        );

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(investmentCategory));
        when(assetRepository.findById(2L)).thenReturn(Optional.of(targetAsset));

        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이체 또는 저축/투자 거래에는 출금 자산(From)을 필수 선택해야 합니다.");
    }

    @Test
    void 저축투자_카테고리_거래에서_입금자산이_누락되면_예외가_발생한다() {
        // Given
        TransactionRequest request = new TransactionRequest(
                LocalDate.now(),
                new BigDecimal("50000"),
                "적금 납입",
                PaymentMethod.BANK_TRANSFER,
                2L, // categoryId
                true, // isConfirmed
                null, // cardId
                1L, // assetId
                null // toAssetId (To 누락)
        );

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(investmentCategory));
        when(assetRepository.findById(1L)).thenReturn(Optional.of(sourceAsset));

        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이체 또는 저축/투자 거래에는 입금 자산(To)을 필수 선택해야 합니다.");
    }

    @Test
    void 특정_날짜_범위의_거래_목록을_성공적으로_조회한다() {
        // Given
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        java.util.List<Transaction> mockTransactions = java.util.List.of(
                new Transaction(LocalDate.of(2026, 6, 5), new BigDecimal("15000"), "마트", PaymentMethod.CARD, expenseCategory, true, null)
        );

        when(transactionRepository.findByDateBetween(start, end)).thenReturn(mockTransactions);

        // When
        java.util.List<org.example.account.dto.TransactionResponse> result = transactionService.getTransactions(start, end);

        // Then
        org.assertj.core.api.Assertions.assertThat(result).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(result.get(0).amount()).isEqualByComparingTo("15000");
    }
}
