// com/master/mosaique_capital/repository/AssetRepository.java
package com.master.mosaique_capital.repository;

import com.master.mosaique_capital.entity.Asset;
import com.master.mosaique_capital.entity.AssetTypeEntity;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.enums.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findByOwner(User owner);

    List<Asset> findByOwnerAndTypeIn(User owner, Collection<AssetTypeEntity> type);


    /**
     * Trouve les actifs par propriétaire et statut
     */
    List<Asset> findByOwnerAndStatus(User owner, AssetStatus status);

    /**
     * Trouve les actifs par propriétaire, type et statut
     */
    List<Asset> findByOwnerAndTypeInAndStatus(User owner, List<AssetTypeEntity> types, AssetStatus status);

    /**
     * Calcule le patrimoine total pour un statut donné (ex: actifs actifs uniquement)
     */
    @Query("SELECT COALESCE(SUM(a.currentValue), 0) FROM Asset a WHERE a.owner = :owner AND a.status = :status")
    BigDecimal sumTotalPatrimonyByStatus(@Param("owner") User owner, @Param("status") AssetStatus status);

    /**
     * Distribution des actifs par type pour un statut donné
     */
    @Query("SELECT a.type.code as type, a.type.label as label, SUM(a.currentValue) as total " +
            "FROM Asset a WHERE a.owner = :owner AND a.status = :status " +
            "GROUP BY a.type.id, a.type.code, a.type.label")
    List<AssetDistributionProjection> getAssetDistributionByTypeAndStatus(@Param("owner") User owner, @Param("status") AssetStatus status);

    /**
     * Trouve les actifs vendus dans une période donnée
     */
    @Query("SELECT a FROM Asset a WHERE a.owner = :owner AND a.status = :status " +
            "AND a.saleDate BETWEEN :startDate AND :endDate")
    List<Asset> findSoldAssetsByDateRange(@Param("owner") User owner,
                                          @Param("status") AssetStatus status,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * Statistiques des ventes par type d'actif
     */
    @Query("SELECT a.type.code as assetType, a.type.label as assetTypeLabel, " +
            "COUNT(a) as totalSales, SUM(a.salePrice) as totalSaleValue, " +
            "SUM(a.salePrice - COALESCE(a.purchasePrice, a.currentValue)) as totalCapitalGain " +
            "FROM Asset a WHERE a.owner = :owner AND a.status = 'SOLD' " +
            "GROUP BY a.type.id, a.type.code, a.type.label")
    List<SalesStatsByTypeProjection> getSalesStatsByType(@Param("owner") User owner);

    /**
     * Top 10 des meilleures ventes (plus-value)
     */
    @Query("SELECT a FROM Asset a WHERE a.owner = :owner AND a.status = 'SOLD' " +
            "ORDER BY (a.salePrice - COALESCE(a.purchasePrice, a.currentValue)) DESC")
    List<Asset> findTopSalesByCapitalGain(@Param("owner") User owner, org.springframework.data.domain.Pageable pageable);

    /**
     * Actifs proches de leur prix d'achat (pour suggestions de vente)
     */
    @Query("SELECT a FROM Asset a WHERE a.owner = :owner AND a.status = 'ACTIVE' " +
            "AND a.currentValue >= a.purchasePrice * :threshold " +
            "ORDER BY (a.currentValue / a.purchasePrice) DESC")
    List<Asset> findAssetsForSaleSuggestion(@Param("owner") User owner, @Param("threshold") BigDecimal threshold);


    /**
     * Distribution des actifs (existante, mise à jour pour compatibilité)
     */
    @Query("SELECT a.type.code as type, a.type.label as label, SUM(a.currentValue) as total " +
            "FROM Asset a WHERE a.owner = :owner GROUP BY a.type.id, a.type.code, a.type.label")
    List<AssetDistributionProjection> getAssetDistributionByType(@Param("owner") User owner);

    /**
     * Patrimoine total (existante, mise à jour pour compatibilité)
     */
    @Query("SELECT COALESCE(SUM(a.currentValue), 0) FROM Asset a WHERE a.owner = :owner")
    BigDecimal sumTotalPatrimony(@Param("owner") User owner);


    /**
     * Interface de projection pour les résultats de distribution
     */
    interface AssetDistributionProjection {
        String getType();
        String getLabel();
        BigDecimal getTotal();
    }

    /**
     * Interface de projection pour les statistiques de ventes par type
     */
    interface SalesStatsByTypeProjection {
        String getAssetType();
        String getAssetTypeLabel();
        Long getTotalSales();
        BigDecimal getTotalSaleValue();
        BigDecimal getTotalCapitalGain();
    }
}