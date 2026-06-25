package org.example.account.service;

import lombok.RequiredArgsConstructor;
import org.example.account.domain.Category;
import org.example.account.domain.TransactionType;
import org.example.account.domain.YearEndCategory;
import org.example.account.dto.CategoryRequest;
import org.example.account.dto.CategoryResponse;
import org.example.account.repository.BudgetRepository;
import org.example.account.repository.CategoryRepository;
import org.example.account.repository.RecurringTransactionRepository;
import org.example.account.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("카테고리 이름을 입력해 주세요.");
        }
        String trimmedName = request.name().trim();
        if (categoryRepository.existsByName(trimmedName)) {
            throw new IllegalArgumentException("이미 존재하는 카테고리 이름입니다.");
        }
        YearEndCategory yearEndCategory = request.yearEndCategory();
        if (request.type() != TransactionType.EXPENSE) {
            yearEndCategory = YearEndCategory.NONE;
        }
        Category category = new Category(trimmedName, request.type(), yearEndCategory);
        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + id));
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("카테고리 이름을 입력해 주세요.");
        }
        String trimmedName = request.name().trim();
        java.util.Optional<Category> duplicate = categoryRepository.findByName(trimmedName);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new IllegalArgumentException("이미 존재하는 카테고리 이름입니다.");
        }
        YearEndCategory yearEndCategory = request.yearEndCategory();
        if (request.type() != TransactionType.EXPENSE) {
            yearEndCategory = YearEndCategory.NONE;
        }
        category.update(trimmedName, request.type(), yearEndCategory);
        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + id));

        if (transactionRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("해당 카테고리를 사용하는 거래가 존재하여 삭제할 수 없습니다.");
        }
        if (budgetRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("해당 카테고리를 사용하는 예산이 존재하여 삭제할 수 없습니다.");
        }
        if (recurringTransactionRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("해당 카테고리를 사용하는 고정 비용이 존재하여 삭제할 수 없습니다.");
        }

        categoryRepository.delete(category);
    }
}
