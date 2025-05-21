// com/master/mosaique_capital/service/AssetValuationService.java
package com.master.mosaique_capital.service;

import com.master.mosaique_capital.dto.asset.AssetValuationDto;
import com.master.mosaique_capital.entity.Asset;
import com.master.mosaique_capital.entity.AssetValuation;
import com.master.mosaique_capital.exception.ResourceNotFoundException;
import com.master.mosaique_capital.mapper.AssetValuationMapper;
import com.master.mosaique_capital.repository.AssetValuationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.master.mosaique_capital.mapper.AssetMapper;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetValuationService {

    private final AssetValuationRepository valuationRepository;
    private final AssetService assetService;
    private final AssetValuationMapper valuationMapper;
    private final AssetMapper assetMapper;

    @Transactional(readOnly = true)
    public List<AssetValuationDto> getValuationsByAssetId(Long assetId) {
        Asset asset = assetService.findAssetById(assetId);
        // La méthode findAssetById vérifie déjà si l'utilisateur est le propriétaire de l'actif

        List<AssetValuation> valuations = valuationRepository.findByAsset(asset);
        return valuationMapper.toDtoList(valuations);
    }

    @Transactional(readOnly = true)
    public List<AssetValuationDto> getValuationsByAssetIdAndDateRange(Long assetId, LocalDate startDate, LocalDate endDate) {
        Asset asset = assetService.findAssetById(assetId);

        List<AssetValuation> valuations = valuationRepository.findByAssetAndValuationDateBetween(asset, startDate, endDate);
        return valuationMapper.toDtoList(valuations);
    }

    @Transactional(readOnly = true)
    public AssetValuationDto getValuationById(Long id) {
        AssetValuation valuation = findValuationById(id);
        checkAssetOwnership(valuation.getAsset());

        return valuationMapper.toDto(valuation);
    }

    @Transactional
    public AssetValuationDto createValuation(AssetValuationDto dto) {
        Asset asset = assetService.findAssetById(dto.getAssetId());
        checkAssetOwnership(asset);

        AssetValuation valuation = valuationMapper.toEntity(dto);
        AssetValuation savedValuation = valuationRepository.save(valuation);

        // Mise à jour de la valeur actuelle de l'actif
        if (dto.getValuationDate().isEqual(LocalDate.now()) || dto.getValuationDate().isAfter(LocalDate.now())) {
            asset.setCurrentValue(dto.getValue());
            assetService.updateAsset(asset.getId(), assetMapper.toDto(asset));
        }

        return valuationMapper.toDto(savedValuation);
    }

    @Transactional
    public void deleteValuation(Long id) {
        AssetValuation valuation = findValuationById(id);
        checkAssetOwnership(valuation.getAsset());

        valuationRepository.delete(valuation);
    }

    private AssetValuation findValuationById(Long id) {
        return valuationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Valorisation non trouvée avec l'ID: " + id));
    }

    private void checkAssetOwnership(Asset asset) {
        // Cette méthode est déjà présente dans AssetService
        assetService.getAssetById(asset.getId());
    }
}