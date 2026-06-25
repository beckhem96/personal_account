package org.example.account.service;

import org.example.account.domain.Budget;
import org.example.account.domain.Category;
import org.example.account.domain.TransactionType;
import org.example.account.dto.BudgetRequest;
import org.example.account.dto.BudgetResponse;
import org.example.account.repository.BudgetRepository;
import org.example.account.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BudgetService budgetService;

    private Category category;
    private Budget budget;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        category = new Category("식비", TransactionType.EXPENSE);
        budget = new Budget(2026, 6, new BigDecimal("500000"), category);
    }

    @Test
    void 신규_예산을_설정하고_저장한다() {
        // Given
        BudgetRequest request = new BudgetRequest(2026, 6, 1L, new BigDecimal("500000"));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(budgetRepository.findByYearAndMonthAndCategoryId(2026, 6, 1L)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);

        // When
        BudgetResponse response = budgetService.setBudget(request);

        // Then
        assertThat(response.amount()).isEqualByComparingTo("500000");
        assertThat(response.categoryName()).isEqualTo("식비");
    }

    @Test
    void 이미_존재하는_예산을_수정하고_저장한다() {
        // Given
        BudgetRequest request = new BudgetRequest(2026, 6, 1L, new BigDecimal("600000"));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(budgetRepository.findByYearAndMonthAndCategoryId(2026, 6, 1L)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);

        // When
        BudgetResponse response = budgetService.setBudget(request);

        // Then
        assertThat(response.amount()).isEqualByComparingTo("600000");
    }

    @Test
    void 월별_설정된_예산_목록을_성공적으로_조회한다() {
        // Given
        when(budgetRepository.findByYearAndMonth(2026, 6)).thenReturn(List.of(budget));

        // When
        List<BudgetResponse> response = budgetService.getMonthlyBudgets(2026, 6);

        // Then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).categoryName()).isEqualTo("식비");
    }
}
