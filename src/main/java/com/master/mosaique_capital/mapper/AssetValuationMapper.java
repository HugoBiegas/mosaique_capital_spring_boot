// com/master/mosaique_capital/mapper/AssetValuationMapper.java
package com.master.mosaique_capital.mapper;

import com.master.mosaique_capital.dto.asset.AssetValuationDto;
import com.master.mosaique_capital.entity.Asset;
import com.master.mosaique_capital.entity.AssetValuation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AssetValuationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "asset", source = "assetId")
    @Mapping(target = "createdAt", ignore = true)
    AssetValuation toEntity(AssetValuationDto dto);

    @Mapping(target = "assetId", source = "asset.id")
    AssetValuationDto toDto(AssetValuation valuation);

    List<AssetValuationDto> toDtoList(List<AssetValuation> valuations);

    default Asset mapAssetId(Long assetId) {
        if (assetId == null) {
            return null;
        }
        Asset asset = new Asset();
        asset.setId(assetId);
        return asset;
    }
}