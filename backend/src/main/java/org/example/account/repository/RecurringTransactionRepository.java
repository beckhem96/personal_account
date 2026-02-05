package org.example.account.repository;

import org.example.account.domain.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {
    boolean existsByCategoryId(Long categoryId);
}
