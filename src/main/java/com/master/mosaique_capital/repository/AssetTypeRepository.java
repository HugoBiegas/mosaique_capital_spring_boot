// com/master/mosaique_capital/repository/AssetTypeRepository.java
package com.master.mosaique_capital.repository;

import com.master.mosaique_capital.entity.AssetTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetTypeRepository extends JpaRepository<AssetTypeEntity, Long> {
    Optional<AssetTypeEntity> findByCode(String code);
}