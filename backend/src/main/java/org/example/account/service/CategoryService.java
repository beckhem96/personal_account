package org.example.account.service;

import lombok.RequiredArgsConstructor;
import org.example.account.domain.Category;
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
        Category category = new Category(request.name(), request.type());
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
        category.update(request.name(), request.type());
        return CategoryResponse.from(category);
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
