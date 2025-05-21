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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final AssetTypeRepository assetTypeRepository; // Nouveau repository
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
        List<Asset> assets = assetRepository.findByOwnerAndType(currentUser, type);
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

        // Récupérer le type d'actif de la base de données
        AssetTypeEntity assetType = assetTypeRepository.findByCode(dto.getType().name())
                .orElseThrow(() -> new ResourceNotFoundException("Type d'actif non trouvé: " + dto.getType().name()));

        // Créer et configurer l'entité Asset
        Asset asset = assetMapper.toEntity(dto);
        asset.setType(assetType); // Assigner directement l'entité récupérée
        asset.setOwner(currentUser);

        Asset savedAsset = assetRepository.save(asset);
        return assetMapper.toDto(savedAsset);
    }

    @Transactional
    public AssetDto updateAsset(Long id, AssetDto dto) {
        Asset asset = findAssetById(id);
        checkAssetOwnership(asset);

        // Si le type change, récupérer le nouveau type
        if (dto.getType() != null) {
            AssetTypeEntity assetType = assetTypeRepository.findByCode(dto.getType().name())
                    .orElseThrow(() -> new ResourceNotFoundException("Type d'actif non trouvé: " + dto.getType().name()));
            asset.setType(assetType);
        }

        // Mettre à jour les autres champs
        if (dto.getName() != null) asset.setName(dto.getName());
        if (dto.getDescription() != null) asset.setDescription(dto.getDescription());
        if (dto.getCurrentValue() != null) asset.setCurrentValue(dto.getCurrentValue());
        if (dto.getCurrency() != null) asset.setCurrency(dto.getCurrency());

        Asset updatedAsset = assetRepository.save(asset);
        return assetMapper.toDto(updatedAsset);
    }

    @Transactional
    public void deleteAsset(Long id) {
        Asset asset = findAssetById(id);
        checkAssetOwnership(asset);

        assetRepository.delete(asset);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPatrimony() {
        User currentUser = getCurrentUser();
        BigDecimal total = assetRepository.sumTotalPatrimony(currentUser);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAssetDistribution() {
        User currentUser = getCurrentUser();
        return assetRepository.getAssetDistributionByType(currentUser);
    }

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