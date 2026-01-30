package org.example.account.service;

import lombok.RequiredArgsConstructor;
import org.example.account.domain.Card;
import org.example.account.domain.Category;
import org.example.account.domain.PaymentMethod;
import org.example.account.domain.RecurringTransaction;
import org.example.account.dto.RecurringTransactionRequest;
import org.example.account.dto.RecurringTransactionResponse;
import org.example.account.repository.CardRepository;
import org.example.account.repository.CategoryRepository;
import org.example.account.repository.RecurringTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurringTransactionService {

    private final RecurringTransactionRepository repository;
    private final CategoryRepository categoryRepository;
    private final CardRepository cardRepository;

    @Transactional
    public RecurringTransactionResponse create(RecurringTransactionRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Card card = null;
        if (request.paymentMethod() == PaymentMethod.CARD && request.cardId() != null) {
            card = cardRepository.findById(request.cardId())
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        }

        RecurringTransaction rt = new RecurringTransaction(
                request.name(),
                request.amount(),
                request.dayOfMonth(),
                request.paymentMethod(),
                card,
                category
        );

        return RecurringTransactionResponse.from(repository.save(rt));
    }

    public List<RecurringTransactionResponse> getAll() {
        return repository.findAll().stream()
                .map(RecurringTransactionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
