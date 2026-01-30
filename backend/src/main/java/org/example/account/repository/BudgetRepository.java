package org.example.account.repository;

import org.example.account.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByYearAndMonth(Integer year, Integer month);
    Optional<Budget> findByYearAndMonthAndCategoryId(Integer year, Integer month, Long categoryId);
}
