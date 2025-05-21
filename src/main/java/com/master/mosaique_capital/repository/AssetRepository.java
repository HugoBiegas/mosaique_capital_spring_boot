// com/master/mosaique_capital/repository/AssetRepository.java
package com.master.mosaique_capital.repository;

import com.master.mosaique_capital.entity.Asset;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByOwner(User owner);
    List<Asset> findByOwnerAndType(User owner, AssetType type);

    @Query("SELECT SUM(a.currentValue) FROM Asset a WHERE a.owner = ?1")
    BigDecimal sumTotalPatrimony(User owner);

    @Query("SELECT a.type as type, SUM(a.currentValue) as total FROM Asset a WHERE a.owner = ?1 GROUP BY a.type")
    List<Map<String, Object>> getAssetDistributionByType(User owner);
}