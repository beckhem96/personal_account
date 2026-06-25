package org.example.account.service;

import org.example.account.repository.CategoryRepository;
import org.example.account.repository.TransactionRepository;
import org.example.account.repository.BudgetRepository;
import org.example.account.repository.RecurringTransactionRepository;
import org.example.account.dto.CategoryRequest;
import org.example.account.dto.CategoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void 카테고리_생성_시_이름이_비어있거나_공백이면_예외를_던진다() {
        // Given
        CategoryRequest emptyNameRequest = new CategoryRequest("", org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE);
        CategoryRequest spaceNameRequest = new CategoryRequest("   ", org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE);
        CategoryRequest nullNameRequest = new CategoryRequest(null, org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE);

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
            categoryService.createCategory(emptyNameRequest)
        );
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
            categoryService.createCategory(spaceNameRequest)
        );
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
            categoryService.createCategory(nullNameRequest)
        );
    }

    @Test
    void 카테고리_생성_시_이름이_중복되면_예외를_던진다() {
        // Given
        CategoryRequest request = new CategoryRequest("식비", org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE);
        when(categoryRepository.existsByName("식비")).thenReturn(true);

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
            categoryService.createCategory(request)
        );
    }

    @Test
    void 카테고리_수정_시_이름이_비어있거나_공백이면_예외를_던진다() {
        // Given
        CategoryRequest emptyNameRequest = new CategoryRequest("", org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE);
        CategoryRequest spaceNameRequest = new CategoryRequest("   ", org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE);
        CategoryRequest nullNameRequest = new CategoryRequest(null, org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE);

        org.example.account.domain.Category category = new org.example.account.domain.Category("식비", org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE);
        when(categoryRepository.findById(1L)).thenReturn(java.util.Optional.of(category));

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
            categoryService.updateCategory(1L, emptyNameRequest)
        );
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
            categoryService.updateCategory(1L, spaceNameRequest)
        );
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
            categoryService.updateCategory(1L, nullNameRequest)
        );
    }

    @Test
    void 카테고리_수정_시_다른_카테고리와_이름이_중복되면_예외를_던진다() {
        // Given
        CategoryRequest request = new CategoryRequest("중복이름", org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE);
        
        org.example.account.domain.Category targetCategory = org.mockito.Mockito.spy(new org.example.account.domain.Category("식비", org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE));
        when(targetCategory.getId()).thenReturn(1L);
        
        org.example.account.domain.Category otherCategory = org.mockito.Mockito.spy(new org.example.account.domain.Category("중복이름", org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE));
        when(otherCategory.getId()).thenReturn(2L);

        when(categoryRepository.findById(1L)).thenReturn(java.util.Optional.of(targetCategory));
        when(categoryRepository.findByName("중복이름")).thenReturn(java.util.Optional.of(otherCategory));

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
            categoryService.updateCategory(1L, request)
        );
    }

    @Test
    void 카테고리_수정_시_본인의_기존_이름과_동일하면_중복으로_판단하지_않는다() {
        // Given
        CategoryRequest request = new CategoryRequest("식비", org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE);
        
        org.example.account.domain.Category targetCategory = org.mockito.Mockito.spy(new org.example.account.domain.Category("식비", org.example.account.domain.TransactionType.EXPENSE, org.example.account.domain.YearEndCategory.NONE));
        when(targetCategory.getId()).thenReturn(1L);

        when(categoryRepository.findById(1L)).thenReturn(java.util.Optional.of(targetCategory));
        when(categoryRepository.findByName("식비")).thenReturn(java.util.Optional.of(targetCategory));
        when(categoryRepository.save(org.mockito.ArgumentMatchers.any(org.example.account.domain.Category.class))).thenReturn(targetCategory);

        // When & Then
        CategoryResponse response = categoryService.updateCategory(1L, request);
        assertThat(response.name()).isEqualTo("식비");
    }

    @Test
    void 빈_문자열_및_null_역직렬화_시_NONE으로_매핑된다() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        
        String jsonWithEmpty = "{\"name\":\"테스트\",\"type\":\"EXPENSE\",\"yearEndCategory\":\"\"}";
        String jsonWithNull = "{\"name\":\"테스트\",\"type\":\"EXPENSE\",\"yearEndCategory\":null}";
        String jsonWithoutField = "{\"name\":\"테스트\",\"type\":\"EXPENSE\"}";
        
        CategoryRequest req1 = objectMapper.readValue(jsonWithEmpty, CategoryRequest.class);
        CategoryRequest req2 = objectMapper.readValue(jsonWithNull, CategoryRequest.class);
        CategoryRequest req3 = objectMapper.readValue(jsonWithoutField, CategoryRequest.class);

        assertThat(req1.yearEndCategory()).isEqualTo(org.example.account.domain.YearEndCategory.NONE);
        assertThat(req2.yearEndCategory()).isEqualTo(org.example.account.domain.YearEndCategory.NONE);
        assertThat(req3.yearEndCategory()).isEqualTo(org.example.account.domain.YearEndCategory.NONE);
    }

    @Test
    void 수입_카테고리_생성_시_연말정산분류는_강제로_NONE이_된다() {
        // Given
        CategoryRequest request = new CategoryRequest("수입카테", org.example.account.domain.TransactionType.INCOME, org.example.account.domain.YearEndCategory.TRADITIONAL_MARKET);
        org.example.account.domain.Category expected = new org.example.account.domain.Category("수입카테", org.example.account.domain.TransactionType.INCOME, org.example.account.domain.YearEndCategory.NONE);
        when(categoryRepository.existsByName("수입카테")).thenReturn(false);
        when(categoryRepository.save(org.mockito.ArgumentMatchers.any(org.example.account.domain.Category.class))).thenReturn(expected);

        // When
        CategoryResponse response = categoryService.createCategory(request);

        // Then
        assertThat(response.yearEndCategory()).isEqualTo(org.example.account.domain.YearEndCategory.NONE);
    }

    @Test
    void 이체_카테고리_수정_시_연말정산분류는_강제로_NONE이_된다() {
        // Given
        CategoryRequest request = new CategoryRequest("이체카테", org.example.account.domain.TransactionType.TRANSFER, org.example.account.domain.YearEndCategory.PUBLIC_TRANSPORT);
        
        org.example.account.domain.Category targetCategory = org.mockito.Mockito.spy(new org.example.account.domain.Category("이체카테", org.example.account.domain.TransactionType.TRANSFER, org.example.account.domain.YearEndCategory.NONE));
        when(targetCategory.getId()).thenReturn(1L);

        when(categoryRepository.findById(1L)).thenReturn(java.util.Optional.of(targetCategory));
        when(categoryRepository.save(org.mockito.ArgumentMatchers.any(org.example.account.domain.Category.class))).thenReturn(targetCategory);

        // When
        CategoryResponse response = categoryService.updateCategory(1L, request);

        // Then
        assertThat(response.yearEndCategory()).isEqualTo(org.example.account.domain.YearEndCategory.NONE);
    }
}
