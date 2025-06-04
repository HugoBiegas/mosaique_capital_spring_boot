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

    // ===== ENDPOINTS EXISTANTS =====

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
            @RequestParam(defaultValue = "false") boolean includeSubTypes) {
        return ResponseEntity.ok(assetService.getAssetsByType(type, includeSubTypes));
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

    // ===== NOUVEAUX ENDPOINTS POUR LA GESTION DES VENTES =====

    /**
     * ✅ NOUVEAU : Vendre un actif
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
     * ✅ NOUVEAU : Récupère tous les actifs incluant les vendus
     */
    @GetMapping("/all")
    public ResponseEntity<List<AssetDto>> getAllAssetsIncludingSold() {
        return ResponseEntity.ok(assetService.getAllAssetsIncludingSold());
    }

    /**
     * ✅ NOUVEAU : Récupère uniquement les actifs vendus
     */
    @GetMapping("/sold")
    public ResponseEntity<List<AssetDto>> getSoldAssets() {
        return ResponseEntity.ok(assetService.getSoldAssets());
    }

    /**
     * ✅ NOUVEAU : Statistiques des ventes
     */
    @GetMapping("/sales/statistics")
    public ResponseEntity<Map<String, Object>> getSalesStatistics() {
        return ResponseEntity.ok(assetService.getSalesStatistics());
    }

    // ===== ENDPOINT DE SUPPRESSION MODIFIÉ =====

    /**
     * ✅ SÉCURISÉ : Plus de suppression, redirection vers vente
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

    // ===== ENDPOINTS D'AIDE ET DOCUMENTATION =====

    /**
     * ✅ NOUVEAU : Politique de suppression
     */
    @GetMapping("/help/deletion-policy")
    public ResponseEntity<Map<String, Object>> getDeletionPolicyHelp() {
        Map<String, Object> policy = Map.of(
                "title", "Politique de Suppression d'Actifs",
                "policy", "La suppression définitive d'actifs n'est pas autorisée pour préserver l'historique financier",
                "alternatives", Map.of(
                        "sell", Map.of(
                                "description", "Marquer l'actif comme vendu",
                                "endpoint", "POST /api/assets/{id}/sell",
                                "preserves_history", true
                        ),
                        "suspend", Map.of(
                                "description", "Suspendre temporairement l'actif",
                                "endpoint", "PUT /api/assets/{id}/status",
                                "reversible", true
                        )
                ),
                "benefits", List.of(
                        "Préservation de l'historique des investissements",
                        "Calcul précis des plus-values/moins-values",
                        "Rapports fiscaux complets",
                        "Traçabilité des opérations"
                )
        );

        return ResponseEntity.ok(policy);
    }

    /**
     * ✅ NOUVEAU : Guide de vente d'actifs
     */
    @GetMapping("/help/selling-guide")
    public ResponseEntity<Map<String, Object>> getSellingGuide() {
        Map<String, Object> guide = Map.of(
                "title", "Guide de Vente d'Actifs",
                "steps", List.of(
                        "1. Vérifiez que l'actif est vendable (statut ACTIVE)",
                        "2. Préparez les informations de vente (prix, date, notes)",
                        "3. Appelez l'endpoint POST /api/assets/{id}/sell",
                        "4. L'actif sera marqué comme SOLD avec calcul automatique des plus-values"
                ),
                "required_fields", Map.of(
                        "salePrice", "Prix de vente (obligatoire, doit être > 0)",
                        "saleDate", "Date de vente (optionnel, défaut = aujourd'hui)",
                        "saleNotes", "Notes sur la vente (optionnel)"
                ),
                "calculated_fields", List.of(
                        "capitalGain - Plus/moins-value réalisée",
                        "capitalGainPercentage - Pourcentage de gain",
                        "soldAt - Timestamp de l'opération"
                ),
                "example", Map.of(
                        "method", "POST",
                        "url", "/api/assets/123/sell",
                        "body", Map.of(
                                "salePrice", 15000.00,
                                "saleDate", "2024-03-15",
                                "saleNotes", "Vente suite à rééquilibrage du portefeuille"
                        )
                )
        );

        return ResponseEntity.ok(guide);
    }
}