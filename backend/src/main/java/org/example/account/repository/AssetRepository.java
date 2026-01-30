package org.example.account.repository;

import org.example.account.domain.Asset;
import org.example.account.domain.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByType(AssetType type);
}
