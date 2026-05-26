package org.example.account.service;

import lombok.RequiredArgsConstructor;
import org.example.account.domain.Asset;
import org.example.account.domain.AssetType;
import org.example.account.dto.AssetRequest;
import org.example.account.dto.AssetResponse;
import org.example.account.dto.NetWorthResponse;
import org.example.account.domain.Transaction;
import org.example.account.repository.AssetRepository;
import org.example.account.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public AssetResponse createAsset(AssetRequest request) {
        Asset asset = new Asset(
                request.type(),
                request.name(),
                request.balance(),
                request.purchasePrice()
        );
        return AssetResponse.from(assetRepository.save(asset));
    }

    @Transactional
    public AssetResponse updateAsset(Long id, AssetRequest request) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));
        
        asset.update(request.name(), request.balance(), request.purchasePrice());
        return AssetResponse.from(asset);
    }

    public List<AssetResponse> getAllAssets() {
        return assetRepository.findAll().stream()
                .map(AssetResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AssetResponse setDefaultAsset(Long id) {
        Asset newDefault = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        // 기존 기본 자산 해제
        assetRepository.findByIsDefaultTrue().ifPresent(current -> current.setDefault(false));

        // 새 기본 자산 설정
        newDefault.setDefault(true);
        return AssetResponse.from(newDefault);
    }

    @Transactional
    public void deleteAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        if (asset.isDefault()) {
            throw new IllegalStateException("기본 자산은 삭제할 수 없습니다.");
        }

        // 해당 자산을 참조하는 거래의 FK를 null로 정리
        for (Transaction tx : transactionRepository.findByAssetId(id)) {
            tx.associateAsset(null, tx.getToAsset());
        }
        for (Transaction tx : transactionRepository.findByToAssetId(id)) {
            tx.associateAsset(tx.getAsset(), null);
        }

        assetRepository.delete(asset);
    }

    public NetWorthResponse calculateNetWorth() {
        List<Asset> allAssets = assetRepository.findAll();

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        Map<String, BigDecimal> assetsByType = new HashMap<>();

        for (Asset asset : allAssets) {
            if (asset.getType() == AssetType.DEBT) {
                totalLiabilities = totalLiabilities.add(asset.getBalance());
            } else {
                totalAssets = totalAssets.add(asset.getBalance());
            }

            assetsByType.merge(
                    asset.getType().name(),
                    asset.getBalance(),
                    BigDecimal::add
            );
        }

        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);

        return new NetWorthResponse(totalAssets, totalLiabilities, netWorth, assetsByType);
    }
}
