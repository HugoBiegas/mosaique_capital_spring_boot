// com/master/mosaique_capital/controller/AssetController.java
package com.master.mosaique_capital.controller;

import com.master.mosaique_capital.dto.asset.AssetCreateRequest;
import com.master.mosaique_capital.dto.asset.AssetDto;
import com.master.mosaique_capital.dto.asset.AssetSaleRequest;
import com.master.mosaique_capital.dto.asset.AssetSaleResponse;
import com.master.mosaique_capital.enums.AssetType;
import com.master.mosaique_capital.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Slf4j
public class AssetController {

    private final AssetService assetService;

    /**
     * Récupère tous les actifs actifs (non vendus)
     */
    @GetMapping
    public ResponseEntity<List<AssetDto>> getAllAssets() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    /**
     * Récupère un actif par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AssetDto> getAssetById(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.getAssetById(id));
    }

    /**
     * Récupère les actifs par type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<AssetDto>> getAssetsByType(
            @PathVariable AssetType type,
            @RequestParam(defaultValue = "false") boolean includeSubTypes,
            @RequestParam(defaultValue = "false") boolean includeSold) {
        return ResponseEntity.ok(assetService.getAssetsByType(type, includeSubTypes, includeSold));
    }

    /**
     * Crée un nouvel actif
     */
    @PostMapping
    public ResponseEntity<AssetDto> createAsset(@Valid @RequestBody AssetCreateRequest request) {
        AssetDto createdAsset = assetService.createAsset(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAsset);
    }

    /**
     * Met à jour un actif
     */
    @PutMapping("/{id}")
    public ResponseEntity<AssetDto> updateAsset(@PathVariable Long id, @Valid @RequestBody AssetDto assetDto) {
        return ResponseEntity.ok(assetService.updateAsset(id, assetDto));
    }

    /**
     * Vendre un actif
     */
    @PostMapping("/{id}/sell")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<AssetSaleResponse> sellAsset(
            @PathVariable Long id,
            @Valid @RequestBody AssetSaleRequest saleRequest) {

        log.info("Demande de vente pour l'actif ID: {} - Prix: {}", id, saleRequest.getSalePrice());

        AssetSaleResponse saleResponse = assetService.sellAsset(id, saleRequest);
        return ResponseEntity.ok(saleResponse);
    }

    /**
     * Récupère tous les actifs incluant les vendus
     */
    @GetMapping("/all")
    public ResponseEntity<List<AssetDto>> getAllAssetsIncludingSold() {
        return ResponseEntity.ok(assetService.getAllAssetsIncludingSold());
    }

    /**
     * Récupère uniquement les actifs vendus
     */
    @GetMapping("/sold")
    public ResponseEntity<List<AssetDto>> getSoldAssets() {
        return ResponseEntity.ok(assetService.getSoldAssets());
    }

    /**
     * Statistiques des ventes
     */
    @GetMapping("/sales/statistics")
    public ResponseEntity<Map<String, Object>> getSalesStatistics() {
        return ResponseEntity.ok(assetService.getSalesStatistics());
    }

    /**
     * Plus de suppression, redirection vers vente
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Map<String, String>> deleteAsset(@PathVariable Long id) {
        log.warn("Tentative de suppression de l'actif ID: {} - Opération bloquée", id);

        try {
            assetService.deleteAsset(id);
            // Ne devrait jamais arriver
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            // Réponse informatique avec alternatives
            Map<String, String> response = Map.of(
                    "error", "DELETION_NOT_ALLOWED",
                    "message", e.getMessage(),
                    "alternative", "Utilisez l'endpoint POST /api/assets/" + id + "/sell pour vendre cet actif",
                    "documentation", "/api/assets/help/deletion-policy"
            );

            return ResponseEntity
                    .status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(response);
        }
    }

}