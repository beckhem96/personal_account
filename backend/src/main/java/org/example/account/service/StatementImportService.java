package org.example.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.account.domain.Card;
import org.example.account.domain.Category;
import org.example.account.domain.PaymentMethod;
import org.example.account.domain.Transaction;
import org.example.account.domain.TransactionType;
import org.example.account.dto.StatementImportResponse;
import org.example.account.repository.CardRepository;
import org.example.account.repository.CategoryRepository;
import org.example.account.repository.TransactionRepository;
import org.example.account.statement.CardStatementParser;
import org.example.account.statement.CardStatementParserRegistry;
import org.example.account.statement.ParsedTransaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementImportService {

    private static final String FALLBACK_CATEGORY_NAME = "기타";
    private static final int SUMMARY_LIMIT = 50;

    private final CardRepository cardRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CardStatementParserRegistry parserRegistry;
    private final GeminiCategoryClassifier classifier;

    @Transactional
    public StatementImportResponse importStatement(Long cardId, MultipartFile file) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("카드를 찾을 수 없습니다: " + cardId));

        if (card.getCompany() == null) {
            throw new IllegalArgumentException("카드에 카드사가 지정되어 있지 않습니다. 카드 정보에서 카드사를 먼저 설정하세요.");
        }

        CardStatementParser parser = parserRegistry.get(card.getCompany())
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 카드사입니다: " + card.getCompany()));

        List<ParsedTransaction> parsed;
        try {
            parsed = parser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("파일을 읽을 수 없습니다: " + e.getMessage(), e);
        }

        if (parsed.isEmpty()) {
            return new StatementImportResponse(0, 0, 0, 0, Collections.emptyList());
        }

        List<ParsedTransaction> deduped = filterDuplicates(parsed);
        int skipped = parsed.size() - deduped.size();

        if (deduped.isEmpty()) {
            return new StatementImportResponse(0, skipped, 0, 0, Collections.emptyList());
        }

        List<Category> expenseCategories = categoryRepository.findByType(TransactionType.EXPENSE);
        List<String> categoryNames = expenseCategories.stream().map(Category::getName).collect(Collectors.toList());
        Category fallbackCategory = ensureFallbackCategory(expenseCategories);

        Set<String> merchants = deduped.stream()
                .map(ParsedTransaction::merchant)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, String> classification = classifier.classify(merchants, categoryNames);

        Map<String, Category> categoryByName = expenseCategories.stream()
                .collect(Collectors.toMap(Category::getName, c -> c, (a, b) -> a));
        categoryByName.putIfAbsent(fallbackCategory.getName(), fallbackCategory);

        int imported = 0;
        int failed = 0;
        int unclassified = 0;
        List<StatementImportResponse.ImportedItem> summary = new ArrayList<>();

        for (ParsedTransaction p : deduped) {
            try {
                String categoryName = classification.get(p.merchant());
                Category category = categoryName == null ? fallbackCategory : categoryByName.getOrDefault(categoryName, fallbackCategory);
                if (categoryName == null) {
                    unclassified++;
                }

                List<Transaction> created = expandInstallments(p, card, category);
                transactionRepository.saveAll(created);
                imported += created.size();

                if (summary.size() < SUMMARY_LIMIT) {
                    summary.add(new StatementImportResponse.ImportedItem(
                            p.date(), p.merchant(), p.amount(), category.getName(),
                            p.installmentSeq(), p.installmentMonths()
                    ));
                }
            } catch (Exception e) {
                failed++;
                log.warn("거래 저장 실패: {} - {}", p.merchant(), e.getMessage());
            }
        }

        return new StatementImportResponse(imported, skipped, failed, unclassified, summary);
    }

    private List<ParsedTransaction> filterDuplicates(List<ParsedTransaction> parsed) {
        Set<String> incomingKeys = parsed.stream()
                .map(ParsedTransaction::naturalKey)
                .collect(Collectors.toSet());
        Set<String> existing = new HashSet<>(transactionRepository.findExistingExternalRefs(incomingKeys));
        Set<String> seenInBatch = new HashSet<>();
        List<ParsedTransaction> result = new ArrayList<>(parsed.size());
        for (ParsedTransaction p : parsed) {
            if (existing.contains(p.naturalKey())) continue;
            if (!seenInBatch.add(p.naturalKey())) continue;
            result.add(p);
        }
        return result;
    }

    List<Transaction> expandInstallments(ParsedTransaction p, Card card, Category category) {
        if (!p.isInstallment()) {
            return List.of(buildTransaction(p.date(), p.amount(), p.merchant(), card, category, p.naturalKey(), null, null));
        }

        int months = p.installmentMonths();
        BigDecimal monthly = p.amount().divide(BigDecimal.valueOf(months), 0, RoundingMode.FLOOR);
        BigDecimal accumulated = monthly.multiply(BigDecimal.valueOf(months - 1));
        BigDecimal lastInstallment = p.amount().subtract(accumulated);

        List<Transaction> txs = new ArrayList<>(months);
        for (int i = 1; i <= months; i++) {
            BigDecimal amount = (i == months) ? lastInstallment : monthly;
            LocalDate date = p.date().plusMonths(i - 1);
            String key = String.format("HANA:%s:%s:%s:%d/%d", p.date(), p.merchant(), p.amount().toPlainString(), i, months);
            txs.add(buildTransaction(date, amount, p.merchant(), card, category, key, i, months));
        }
        return txs;
    }

    private Transaction buildTransaction(LocalDate date, BigDecimal amount, String merchant, Card card, Category category, String key, Integer seq, Integer months) {
        String memo = (seq != null && months != null)
                ? String.format("%s (%d/%d)", merchant, seq, months)
                : merchant;
        return new Transaction(date, amount, memo, PaymentMethod.CARD, category, false, card, key);
    }

    private Category ensureFallbackCategory(List<Category> existing) {
        return existing.stream()
                .filter(c -> FALLBACK_CATEGORY_NAME.equals(c.getName()))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(new Category(FALLBACK_CATEGORY_NAME, TransactionType.EXPENSE)));
    }
}
