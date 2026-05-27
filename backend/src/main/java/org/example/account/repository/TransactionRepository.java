package org.example.account.repository;

import org.example.account.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.date >= :date AND t.isConfirmed = false")
    List<Transaction> findFuturePlannedTransactions(@Param("date") LocalDate date);

    List<Transaction> findByCardIdAndDateBetween(Long cardId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByPaymentMethodAndDateBetween(org.example.account.domain.PaymentMethod paymentMethod, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.card.id = :cardId")
    List<Transaction> findByCardId(@Param("cardId") Long cardId);

    boolean existsByRecurringTransactionIdAndDateBetween(Long recurringTransactionId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByRecurringTransactionIdAndIsConfirmedFalseAndDateBetween(Long recurringTransactionId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByAssetId(Long assetId);

    List<Transaction> findByToAssetId(Long toAssetId);

    boolean existsByCategoryId(Long categoryId);

    @Modifying
    @Query("UPDATE Transaction t SET t.recurringTransaction = null WHERE t.recurringTransaction.id = :recurringTransactionId")
    int detachFromRecurringTransaction(@Param("recurringTransactionId") Long recurringTransactionId);

    @Query("SELECT t.externalRef FROM Transaction t WHERE t.externalRef IN :refs")
    List<String> findExistingExternalRefs(@Param("refs") Collection<String> refs);

    @Query("SELECT DISTINCT t FROM Transaction t " +
            "LEFT JOIN FETCH t.category " +
            "LEFT JOIN FETCH t.card " +
            "WHERE t.date BETWEEN :start AND :end AND t.isConfirmed = true")
    List<Transaction> findConfirmedWithJoinsBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
