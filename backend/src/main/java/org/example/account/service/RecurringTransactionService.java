package org.example.account.service;

import lombok.RequiredArgsConstructor;
import org.example.account.domain.Card;
import org.example.account.domain.Category;
import org.example.account.domain.PaymentMethod;
import org.example.account.domain.RecurringTransaction;
import org.example.account.domain.Transaction;
import org.example.account.dto.RecurringTransactionRequest;
import org.example.account.dto.RecurringTransactionResponse;
import org.example.account.repository.CardRepository;
import org.example.account.repository.CategoryRepository;
import org.example.account.repository.RecurringTransactionRepository;
import org.example.account.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurringTransactionService {

    private final RecurringTransactionRepository repository;
    private final CategoryRepository categoryRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

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

        RecurringTransaction saved = repository.save(rt);

        // 고정 비용 생성 시 현재 월의 Transaction도 자동 생성
        createTransactionFromRecurring(saved);

        return RecurringTransactionResponse.from(saved);
    }

    private void createTransactionFromRecurring(RecurringTransaction rt) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        // 해당 월의 마지막 날보다 dayOfMonth가 크면 마지막 날로 설정
        int dayOfMonth = Math.min(rt.getDayOfMonth(), currentMonth.lengthOfMonth());
        LocalDate transactionDate = currentMonth.atDay(dayOfMonth);

        // 이미 지난 날짜인 경우 isConfirmed = true, 아직 안 지난 경우 false (예정)
        boolean isConfirmed = !transactionDate.isAfter(today);

        Transaction transaction = new Transaction(
                transactionDate,
                rt.getAmount(),
                rt.getName() + " (고정비용)",
                rt.getPaymentMethod(),
                rt.getCategory(),
                isConfirmed,
                rt.getCard()
        );

        transactionRepository.save(transaction);
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
