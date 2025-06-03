// com/master/mosaique_capital/repository/AssetRepository.java
package com.master.mosaique_capital.repository;

import com.master.mosaique_capital.entity.Asset;
import com.master.mosaique_capital.entity.AssetTypeEntity;
import com.master.mosaique_capital.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByOwner(User owner);

    List<Asset> findByOwnerAndType(User owner, AssetTypeEntity type);

    @Query("SELECT COALESCE(SUM(a.currentValue), 0) FROM Asset a WHERE a.owner = :owner")
    BigDecimal sumTotalPatrimony(@Param("owner") User owner);

    @Query("SELECT a.type.code as type, a.type.label as label, SUM(a.currentValue) as total " +
            "FROM Asset a WHERE a.owner = :owner GROUP BY a.type.id, a.type.code, a.type.label")
    List<AssetDistributionProjection> getAssetDistributionByType(@Param("owner") User owner);

    List<Asset> findByOwnerAndTypeIn (User owner, List<AssetTypeEntity> types);

    // Interface de projection pour les résultats de distribution
    interface AssetDistributionProjection {
        String getType();
        String getLabel();
        BigDecimal getTotal();
    }
}