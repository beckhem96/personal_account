package org.example.account.repository;

import org.example.account.domain.MyStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MyStockRepository extends JpaRepository<MyStock, Long> {
    Optional<MyStock> findByTicker(String ticker);
    boolean existsByTicker(String ticker);
}
