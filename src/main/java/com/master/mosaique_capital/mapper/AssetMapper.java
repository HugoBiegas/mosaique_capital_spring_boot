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

    /**
     * ⭐ CORRECTION CRITIQUE : Ignorer le champ 'type' lors du mapping
     * Le service AssetService se charge de définir le type manuellement
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "type", ignore = true)  // ⭐ AJOUTÉ : Ignore le type
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

    default AssetType mapEntityToEnum(AssetTypeEntity value) {
        if (value == null || value.getCode() == null) {
            return null;
        }

        try {
            return AssetType.valueOf(value.getCode());
        } catch (IllegalArgumentException e) {
            // Log de l'erreur pour debugging
            System.err.println("⚠️ Code AssetType non trouvé dans l'enum: " + value.getCode());
            return null;
        }
    }

    /**
     * Mapping manuel pour le champ type (entity vers enum)
     */
    default AssetType map(AssetTypeEntity value) {
        return mapEntityToEnum(value);
    }
}