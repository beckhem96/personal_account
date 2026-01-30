package org.example.account.controller;

import lombok.RequiredArgsConstructor;
import org.example.account.dto.AssetRequest;
import org.example.account.dto.AssetResponse;
import org.example.account.dto.NetWorthResponse;
import org.example.account.service.AssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<AssetResponse> createAsset(@RequestBody AssetRequest request) {
        return ResponseEntity.ok(assetService.createAsset(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> updateAsset(@PathVariable Long id, @RequestBody AssetRequest request) {
        return ResponseEntity.ok(assetService.updateAsset(id, request));
    }

    @GetMapping
    public ResponseEntity<List<AssetResponse>> getAllAssets() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    @GetMapping("/net-worth")
    public ResponseEntity<NetWorthResponse> getNetWorth() {
        return ResponseEntity.ok(assetService.calculateNetWorth());
    }
}
