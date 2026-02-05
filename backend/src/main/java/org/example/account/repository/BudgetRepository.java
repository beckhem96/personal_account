package org.example.account.repository;

import org.example.account.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByYearAndMonth(Integer year, Integer month);
    Optional<Budget> findByYearAndMonthAndCategoryId(Integer year, Integer month, Long categoryId);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Budget b WHERE (b.year * 12 + b.month) >= :startTotalMonths AND (b.year * 12 + b.month) <= :endTotalMonths")
    List<Budget> findBudgetsBetween(Integer startTotalMonths, Integer endTotalMonths);

    boolean existsByCategoryId(Long categoryId);
}
