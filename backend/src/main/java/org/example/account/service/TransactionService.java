package org.example.account.service;

import lombok.RequiredArgsConstructor;
import org.example.account.domain.Card;
import org.example.account.domain.Category;
import org.example.account.domain.PaymentMethod;
import org.example.account.domain.Transaction;
import org.example.account.dto.TransactionRequest;
import org.example.account.dto.TransactionResponse;
import org.example.account.repository.CardRepository;
import org.example.account.repository.CategoryRepository;
import org.example.account.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CardRepository cardRepository;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Card card = null;
        if (request.paymentMethod() == PaymentMethod.CARD && request.cardId() != null) {
            card = cardRepository.findById(request.cardId())
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        }

        boolean confirmed = request.isConfirmed() != null ? request.isConfirmed() : true;

        Transaction transaction = new Transaction(
                request.date(),
                request.amount(),
                request.memo(),
                request.paymentMethod(),
                category,
                confirmed,
                card
        );

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Card card = null;
        if (request.paymentMethod() == PaymentMethod.CARD && request.cardId() != null) {
            card = cardRepository.findById(request.cardId())
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        }

        transaction.update(
                request.date(),
                request.amount(),
                request.memo(),
                request.paymentMethod(),
                category,
                card
        );

        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        transactionRepository.deleteById(id);
    }

    @Transactional
    public TransactionResponse confirmTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        transaction.confirm();
        return TransactionResponse.from(transaction);
    }

    public List<TransactionResponse> getTransactions(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByDateBetween(startDate, endDate).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsByPaymentMethod(PaymentMethod paymentMethod, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByPaymentMethodAndDateBetween(paymentMethod, startDate, endDate).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getFuturePlannedTransactions() {
        return transactionRepository.findFuturePlannedTransactions(LocalDate.now()).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsByCard(Long cardId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByCardIdAndDateBetween(cardId, startDate, endDate).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsByCard(Long cardId) {
        return transactionRepository.findByCardId(cardId).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }
}
