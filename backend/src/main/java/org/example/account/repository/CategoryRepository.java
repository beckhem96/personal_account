package org.example.account.repository;

import org.example.account.domain.Category;
import org.example.account.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    List<Category> findByType(TransactionType type);
}
