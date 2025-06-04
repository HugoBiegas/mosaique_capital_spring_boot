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

import java.util.List;

@Mapper(componentModel = "spring")
public interface AssetMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Asset toEntity(AssetCreateRequest dto);

    /**
     * Mapping de l'entité vers DTO avec conversion du type
     */
    @Mapping(target = "type", source = "type")
    AssetDto toDto(Asset asset);

    List<AssetDto> toDtoList(List<Asset> assets);

    /**
     * Mise à jour d'une entité existante
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(AssetDto dto, @MappingTarget Asset asset);

    /**
     * ✅ CORRECTION : Une seule méthode de mapping pour éviter l'ambiguïté
     * Mapping manuel pour le champ type (entity vers enum)
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