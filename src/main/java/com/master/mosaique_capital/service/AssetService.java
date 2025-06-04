// com/master/mosaique_capital/service/AssetService.java
package com.master.mosaique_capital.service;

import com.master.mosaique_capital.dto.asset.AssetCreateRequest;
import com.master.mosaique_capital.dto.asset.AssetDto;
import com.master.mosaique_capital.dto.asset.AssetSaleRequest;
import com.master.mosaique_capital.dto.asset.AssetSaleResponse;
import com.master.mosaique_capital.entity.Asset;
import com.master.mosaique_capital.entity.AssetTypeEntity;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.enums.AssetStatus;
import com.master.mosaique_capital.enums.AssetType;
import com.master.mosaique_capital.exception.ResourceNotFoundException;
import com.master.mosaique_capital.mapper.AssetMapper;
import com.master.mosaique_capital.repository.AssetRepository;
import com.master.mosaique_capital.repository.AssetTypeRepository;
import com.master.mosaique_capital.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetService {

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final AssetMapper assetMapper;

    /**
     * Récupère tous les actifs actifs (non vendus) de l'utilisateur
     */
    @Transactional(readOnly = true)
    public List<AssetDto> getAllAssets() {
        User currentUser = getCurrentUser();
        List<Asset> assets = assetRepository.findByOwnerAndStatus(currentUser, AssetStatus.ACTIVE);
        return assetMapper.toDtoList(assets);
    }

    /**
     * Récupère tous les actifs incluant les vendus
     */
    @Transactional(readOnly = true)
    public List<AssetDto> getAllAssetsIncludingSold() {
        User currentUser = getCurrentUser();
        List<Asset> assets = assetRepository.findByOwner(currentUser);
        return assetMapper.toDtoList(assets);
    }

    /**
     * Récupère uniquement les actifs vendus
     */
    @Transactional(readOnly = true)
    public List<AssetDto> getSoldAssets() {
        User currentUser = getCurrentUser();
        List<Asset> assets = assetRepository.findByOwnerAndStatus(currentUser, AssetStatus.SOLD);
        return assetMapper.toDtoList(assets);
    }

    @Transactional(readOnly = true)
    public List<AssetDto> getAssetsByType(AssetType type, boolean includeSubTypes, boolean includeSold) {
        User currentUser = getCurrentUser();

        List<AssetTypeEntity> assetTypeEntities = includeSubTypes
                ? assetTypeRepository.findByCodeStartingWith(type.name())
                : List.of(assetTypeRepository.findByCode(type.name())
                .orElseThrow(() -> new ResourceNotFoundException("Type d'actif non trouvé: " + type.name())));

        List<Asset> assets = includeSold
                ? assetRepository.findByOwnerAndTypeIn(currentUser, assetTypeEntities)
                : assetRepository.findByOwnerAndTypeInAndStatus(currentUser, assetTypeEntities, AssetStatus.ACTIVE);

        return assetMapper.toDtoList(assets);
    }
    @Transactional(readOnly = true)
    public AssetDto getAssetById(Long id) {
        Asset asset = findAssetById(id);
        checkAssetOwnership(asset);
        return assetMapper.toDto(asset);
    }

    @Transactional
    public AssetDto createAsset(AssetCreateRequest dto) {
        User currentUser = getCurrentUser();

        AssetTypeEntity assetType = assetTypeRepository.findByCode(dto.getType().name())
                .orElseThrow(() -> new ResourceNotFoundException("Type d'actif non trouvé: " + dto.getType().name()));

        Asset asset = assetMapper.toEntity(dto);
        asset.setType(assetType);
        asset.setOwner(currentUser);
        asset.setStatus(AssetStatus.ACTIVE);

        // Si un prix d'achat n'est pas spécifié, utiliser la valeur actuelle
        if (asset.getPurchasePrice() == null) {
            asset.setPurchasePrice(asset.getCurrentValue());
        }

        // Si une date d'achat n'est pas spécifiée, utiliser la date actuelle
        if (asset.getPurchaseDate() == null) {
            asset.setPurchaseDate(LocalDate.now());
        }

        Asset savedAsset = assetRepository.save(asset);
        log.info("Nouvel actif créé: {} (ID: {})", savedAsset.getName(), savedAsset.getId());

        return assetMapper.toDto(savedAsset);
    }

    @Transactional
    public AssetDto updateAsset(Long id, AssetDto dto) {
        Asset asset = findAssetById(id);
        checkAssetOwnership(asset);

        // ✅ SÉCURITÉ : Empêcher la modification d'actifs vendus
        if (asset.isSold()) {
            throw new IllegalStateException("Impossible de modifier un actif vendu");
        }

        if (dto.getType() != null) {
            AssetTypeEntity assetType = assetTypeRepository.findByCode(dto.getType().name())
                    .orElseThrow(() -> new ResourceNotFoundException("Type d'actif non trouvé: " + dto.getType().name()));
            asset.setType(assetType);
        }

        if (dto.getName() != null) asset.setName(dto.getName());
        if (dto.getDescription() != null) asset.setDescription(dto.getDescription());
        if (dto.getCurrentValue() != null) asset.setCurrentValue(dto.getCurrentValue());
        if (dto.getCurrency() != null) asset.setCurrency(dto.getCurrency());
        if (dto.getPurchasePrice() != null) asset.setPurchasePrice(dto.getPurchasePrice());
        if (dto.getPurchaseDate() != null) asset.setPurchaseDate(dto.getPurchaseDate());

        Asset updatedAsset = assetRepository.save(asset);
        log.info("Actif mis à jour: {} (ID: {})", updatedAsset.getName(), updatedAsset.getId());

        return assetMapper.toDto(updatedAsset);
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Vendre un actif
     */
    @Transactional
    public AssetSaleResponse sellAsset(Long id, AssetSaleRequest saleRequest) {
        Asset asset = findAssetById(id);
        checkAssetOwnership(asset);

        // Vérifications métier
        if (!asset.canBeSold()) {
            throw new IllegalStateException("Cet actif ne peut pas être vendu. Statut actuel: " + asset.getStatus());
        }

        // Validation du prix de vente
        if (saleRequest.getSalePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix de vente doit être positif");
        }

        // Effectuer la vente
        asset.sell(saleRequest.getSalePrice(), saleRequest.getSaleDate(), saleRequest.getSaleNotes());
        Asset soldAsset = assetRepository.save(asset);

        log.info("Actif vendu: {} (ID: {}) - Prix: {} {}",
                soldAsset.getName(), soldAsset.getId(),
                saleRequest.getSalePrice(), asset.getCurrency());

        // Construire la réponse
        AssetSaleResponse response = AssetSaleResponse.builder()
                .assetId(soldAsset.getId())
                .assetName(soldAsset.getName())
                .status(soldAsset.getStatus())
                .salePrice(soldAsset.getSalePrice())
                .saleDate(soldAsset.getSaleDate())
                .saleNotes(soldAsset.getSaleNotes())
                .capitalGain(soldAsset.calculateCapitalGain())
                .soldAt(LocalDateTime.now())
                .build();

        // Calculer le pourcentage de plus-value
        response.calculateCapitalGainPercentage(soldAsset.getPurchasePrice());

        return response;
    }

    /**
     * ✅ MÉTHODE SÉCURISÉE : Plus de suppression, redirection vers vente
     */
    @Transactional
    public void deleteAsset(Long id) {
        Asset asset = findAssetById(id);
        checkAssetOwnership(asset);

        // ✅ SÉCURITÉ : Empêcher la suppression définitive
        throw new UnsupportedOperationException(
                "La suppression d'actifs n'est pas autorisée. " +
                        "Utilisez l'endpoint de vente pour marquer l'actif comme vendu."
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPatrimony() {
        User currentUser = getCurrentUser();
        // Calculer uniquement sur les actifs actifs
        return assetRepository.sumTotalPatrimonyByStatus(currentUser, AssetStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAssetDistribution() {
        User currentUser = getCurrentUser();
        List<AssetRepository.AssetDistributionProjection> projections =
                assetRepository.getAssetDistributionByTypeAndStatus(currentUser, AssetStatus.ACTIVE);

        return projections.stream()
                .map(projection -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("type", projection.getType());
                    result.put("label", projection.getLabel());
                    result.put("total", projection.getTotal());
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Statistiques des ventes
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSalesStatistics() {
        User currentUser = getCurrentUser();

        List<Asset> soldAssets = assetRepository.findByOwnerAndStatus(currentUser, AssetStatus.SOLD);

        BigDecimal totalSaleValue = soldAssets.stream()
                .map(Asset::getSalePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCapitalGain = soldAssets.stream()
                .map(Asset::calculateCapitalGain)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSoldAssets", soldAssets.size());
        stats.put("totalSaleValue", totalSaleValue);
        stats.put("totalCapitalGain", totalCapitalGain);
        stats.put("averageSalePrice", soldAssets.isEmpty() ? BigDecimal.ZERO :
                totalSaleValue.divide(BigDecimal.valueOf(soldAssets.size()), 2, java.math.RoundingMode.HALF_UP));

        return stats;
    }

    // ===== Méthodes utilitaires =====

    public Asset findAssetById(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actif non trouvé avec l'ID: " + id));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    private void checkAssetOwnership(Asset asset) {
        User currentUser = getCurrentUser();
        if (!asset.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous n'avez pas les droits pour accéder à cet actif");
        }
    }
}