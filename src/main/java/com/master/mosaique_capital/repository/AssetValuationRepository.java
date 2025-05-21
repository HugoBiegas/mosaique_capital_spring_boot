// com/master/mosaique_capital/repository/AssetValuationRepository.java
package com.master.mosaique_capital.repository;

import com.master.mosaique_capital.entity.Asset;
import com.master.mosaique_capital.entity.AssetValuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AssetValuationRepository extends JpaRepository<AssetValuation, Long> {
    List<AssetValuation> findByAsset(Asset asset);
    List<AssetValuation> findByAssetAndValuationDateBetween(Asset asset, LocalDate startDate, LocalDate endDate);

    @Query("SELECT av FROM AssetValuation av WHERE av.asset = ?1 ORDER BY av.valuationDate DESC")
    List<AssetValuation> findLatestValuations(Asset asset, int limit);
}