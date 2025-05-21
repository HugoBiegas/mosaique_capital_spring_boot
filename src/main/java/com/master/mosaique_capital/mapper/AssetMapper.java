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
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Asset toEntity(AssetCreateRequest dto);

    AssetDto toDto(Asset asset);

    List<AssetDto> toDtoList(List<Asset> assets);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(AssetDto dto, @MappingTarget Asset asset);

    // Méthode de conversion de l'énumération vers l'entité
    default AssetTypeEntity map(AssetType value) {
        if (value == null) {
            return null;
        }

        AssetTypeEntity typeEntity = new AssetTypeEntity();
        typeEntity.setCode(value.name());
        typeEntity.setLabel(value.getLabel());

        return typeEntity;
    }

    // Méthode de conversion de l'entité vers l'énumération
    default AssetType map(AssetTypeEntity value) {
        if (value == null) {
            return null;
        }

        try {
            return AssetType.valueOf(value.getCode());
        } catch (IllegalArgumentException e) {
            return null; // Gérer le cas où le code n'existe pas dans l'énumération
        }
    }
}