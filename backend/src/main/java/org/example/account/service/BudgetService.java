package org.example.account.service;

import lombok.RequiredArgsConstructor;
import org.example.account.domain.Budget;
import org.example.account.domain.Category;
import org.example.account.dto.BudgetRequest;
import org.example.account.dto.BudgetResponse;
import org.example.account.repository.BudgetRepository;
import org.example.account.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public BudgetResponse setBudget(BudgetRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Budget budget = budgetRepository.findByYearAndMonthAndCategoryId(request.year(), request.month(), request.categoryId())
                .map(existing -> {
                    existing.updateAmount(request.amount());
                    return existing;
                })
                .orElseGet(() -> new Budget(request.year(), request.month(), request.amount(), category));

        Budget saved = budgetRepository.save(budget);
        return BudgetResponse.from(saved);
    }

    public List<BudgetResponse> getMonthlyBudgets(Integer year, Integer month) {
        return budgetRepository.findByYearAndMonth(year, month).stream()
                .map(BudgetResponse::from)
                .collect(Collectors.toList());
    }

    public List<BudgetResponse> getBudgetsByPeriod(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        int startTotal = startDate.getYear() * 12 + startDate.getMonthValue();
        int endTotal = endDate.getYear() * 12 + endDate.getMonthValue();
        
        return budgetRepository.findBudgetsBetween(startTotal, endTotal).stream()
                .map(BudgetResponse::from)
                .collect(Collectors.toList());
    }
}
