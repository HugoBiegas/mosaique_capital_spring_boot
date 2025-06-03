// com/master/mosaique_capital/service/AssetService.java
package com.master.mosaique_capital.service;

import com.master.mosaique_capital.dto.asset.AssetCreateRequest;
import com.master.mosaique_capital.dto.asset.AssetDto;
import com.master.mosaique_capital.entity.Asset;
import com.master.mosaique_capital.entity.AssetTypeEntity;
import com.master.mosaique_capital.entity.User;
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

    @Transactional(readOnly = true)
    public List<AssetDto> getAllAssets() {
        User currentUser = getCurrentUser();
        List<Asset> assets = assetRepository.findByOwner(currentUser);
        return assetMapper.toDtoList(assets);
    }

    @Transactional(readOnly = true)
    public List<AssetDto> getAssetsByType(AssetType type) {
        User currentUser = getCurrentUser();
        AssetTypeEntity assetTypeEntity = findAssetTypeByCode(type.name());

        List<Asset> assets = assetRepository.findByOwnerAndType(currentUser, assetTypeEntity);
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
        log.info("🔄 Création d'un nouvel asset de type: {}", dto.getType());

        try {
            // 1. Récupérer l'utilisateur actuel
            User currentUser = getCurrentUser();
            log.debug("👤 Utilisateur: {}", currentUser.getUsername());

            // 2. Récupérer le type d'actif de la base de données
            AssetTypeEntity assetType = findAssetTypeByCode(dto.getType().name());
            log.debug("📂 Type d'asset trouvé: {} (ID: {})", assetType.getCode(), assetType.getId());

            // 3. Créer l'entité Asset via le mapper (sans le type)
            Asset asset = assetMapper.toEntity(dto);

            // 4. Définir manuellement les champs requis
            asset.setType(assetType);
            asset.setOwner(currentUser);

            log.debug("💰 Asset à sauvegarder: nom={}, valeur={}, type={}",
                    asset.getName(), asset.getCurrentValue(), asset.getType().getCode());

            // 5. Sauvegarder l'asset
            Asset savedAsset = assetRepository.save(asset);
            log.info("✅ Asset créé avec succès (ID: {})", savedAsset.getId());

            // 6. Retourner le DTO
            return assetMapper.toDto(savedAsset);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'asset: {}", e.getMessage(), e);
            throw e; // Re-lancer l'exception pour que Spring la gère
        }
    }


    @Transactional
    public AssetDto updateAsset(Long id, AssetDto dto) {
        log.info("🔄 Mise à jour de l'asset ID: {}", id);

        Asset asset = findAssetById(id);
        checkAssetOwnership(asset);

        try {
            // Mise à jour du type si nécessaire
            if (dto.getType() != null) {
                AssetTypeEntity assetType = findAssetTypeByCode(dto.getType().name());
                asset.setType(assetType);
                log.debug("📂 Type mis à jour: {}", assetType.getCode());
            }

            // Mise à jour des autres champs
            if (dto.getName() != null) {
                asset.setName(dto.getName());
            }
            if (dto.getDescription() != null) {
                asset.setDescription(dto.getDescription());
            }
            if (dto.getCurrentValue() != null) {
                asset.setCurrentValue(dto.getCurrentValue());
            }
            if (dto.getCurrency() != null) {
                asset.setCurrency(dto.getCurrency());
            }

            Asset updatedAsset = assetRepository.save(asset);
            log.info("✅ Asset mis à jour avec succès");

            return assetMapper.toDto(updatedAsset);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour de l'asset: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void deleteAsset(Long id) {
        Asset asset = findAssetById(id);
        checkAssetOwnership(asset);
        assetRepository.delete(asset);
        log.info("🗑️ Asset supprimé (ID: {})", id);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPatrimony() {
        User currentUser = getCurrentUser();
        return assetRepository.sumTotalPatrimony(currentUser);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAssetDistribution() {
        User currentUser = getCurrentUser();
        List<AssetRepository.AssetDistributionProjection> projections =
                assetRepository.getAssetDistributionByType(currentUser);

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

    // ===== MÉTHODES UTILITAIRES AMÉLIORÉES =====

    /**
     * ⭐ MÉTHODE AMÉLIORÉE : Recherche d'asset avec logs détaillés
     */
    public Asset findAssetById(Long id) {
        log.debug("🔍 Recherche asset ID: {}", id);
        return assetRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("❌ Asset non trouvé (ID: {})", id);
                    return new ResourceNotFoundException("Actif non trouvé avec l'ID: " + id);
                });
    }

    /**
     * ⭐ NOUVELLE MÉTHODE : Recherche de type d'asset avec logs détaillés
     */
    private AssetTypeEntity findAssetTypeByCode(String code) {
        log.debug("🔍 Recherche type d'asset: {}", code);
        return assetTypeRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.error("❌ Type d'actif non trouvé: {}", code);
                    log.info("📋 Types disponibles: {}",
                            assetTypeRepository.findAll().stream()
                                    .map(AssetTypeEntity::getCode)
                                    .collect(Collectors.toList()));
                    return new ResourceNotFoundException("Type d'actif non trouvé: " + code);
                });
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.debug("🔍 Recherche utilisateur: {}", username);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("❌ Utilisateur non trouvé: {}", username);
                    return new ResourceNotFoundException("Utilisateur non trouvé");
                });
    }

    private void checkAssetOwnership(Asset asset) {
        User currentUser = getCurrentUser();
        if (!asset.getOwner().getId().equals(currentUser.getId())) {
            log.warn("🚫 Tentative d'accès non autorisé à l'asset {} par l'utilisateur {}",
                    asset.getId(), currentUser.getUsername());
            throw new AccessDeniedException("Vous n'avez pas les droits pour accéder à cet actif");
        }
    }
}