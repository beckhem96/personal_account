package org.example.account.repository;

import org.example.account.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.date >= :date AND t.isConfirmed = false")
    List<Transaction> findFuturePlannedTransactions(@Param("date") LocalDate date);

    List<Transaction> findByCardIdAndDateBetween(Long cardId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.card.id = :cardId")
    List<Transaction> findByCardId(@Param("cardId") Long cardId);

    boolean existsByRecurringTransactionIdAndDateBetween(Long recurringTransactionId, LocalDate startDate, LocalDate endDate);
}
