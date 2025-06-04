// com/master/mosaique_capital/mapper/AssetMapper.java
package com.master.mosaique_capital.mapper;

import com.master.mosaique_capital.dto.asset.AssetCreateRequest;
import com.master.mosaique_capital.dto.asset.AssetDto;
import com.master.mosaique_capital.entity.Asset;
import com.master.mosaique_capital.entity.AssetTypeEntity;
import com.master.mosaique_capital.enums.AssetType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.AfterMapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AssetMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "saleDate", ignore = true)
    @Mapping(target = "salePrice", ignore = true)
    @Mapping(target = "saleNotes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Asset toEntity(AssetCreateRequest dto);

    /**
     * Mapping de l'entité vers DTO avec conversion du type et calcul de la plus-value
     */
    @Mapping(target = "type", source = "type")
    @Mapping(target = "capitalGain", ignore = true)
    AssetDto toDto(Asset asset);

    List<AssetDto> toDtoList(List<Asset> assets);

    /**
     * Mise à jour d'une entité existante
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "saleDate", ignore = true)
    @Mapping(target = "salePrice", ignore = true)
    @Mapping(target = "saleNotes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(AssetDto dto, @MappingTarget Asset asset);

    /**
     * ✅ Calcul automatique de la plus-value après mapping
     */
    @AfterMapping
    default void calculateCapitalGain(@MappingTarget AssetDto dto, Asset asset) {
        if (asset != null) {
            dto.setCapitalGain(asset.calculateCapitalGain());
        }
    }

    /**
     * ✅ Mapping manuel pour le champ type (entity vers enum)
     */
    default AssetType map(AssetTypeEntity assetTypeEntity) {
        if (assetTypeEntity == null || assetTypeEntity.getCode() == null) {
            return null;
        }

        try {
            return AssetType.valueOf(assetTypeEntity.getCode());
        } catch (IllegalArgumentException e) {
            // Log de l'erreur pour debugging
            System.err.println("⚠️ Code AssetType non trouvé dans l'enum: " + assetTypeEntity.getCode());
            return null;
        }
    }
}