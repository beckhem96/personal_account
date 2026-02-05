package org.example.account.config;

import lombok.RequiredArgsConstructor;
import org.example.account.domain.Asset;
import org.example.account.domain.AssetType;
import org.example.account.domain.Category;
import org.example.account.domain.TransactionType;
import org.example.account.repository.AssetRepository;
import org.example.account.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final AssetRepository assetRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            List<Category> defaults = Arrays.asList(
                    new Category("월급", TransactionType.INCOME),
                    new Category("부수입", TransactionType.INCOME),
                    new Category("식비", TransactionType.EXPENSE),
                    new Category("교통/차량", TransactionType.EXPENSE),
                    new Category("주거/통신", TransactionType.EXPENSE),
                    new Category("쇼핑/생활", TransactionType.EXPENSE),
                    new Category("문화/여가", TransactionType.EXPENSE),
                    new Category("의료/건강", TransactionType.EXPENSE),
                    new Category("저축/투자", TransactionType.EXPENSE),
                    new Category("이체", TransactionType.TRANSFER)
            );

            categoryRepository.saveAll(defaults);
            System.out.println("Initialized default categories.");
        }

        if (assetRepository.count() == 0) {
            Asset defaultCash = new Asset(AssetType.CASH, "현금", BigDecimal.ZERO, null, true);
            assetRepository.save(defaultCash);
            System.out.println("Initialized default cash asset.");
        }
    }
}
