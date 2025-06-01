// src/test/java/com/master/mosaique_capital/repository/AssetRepositoryTest.java
package com.master.mosaique_capital.repository;

import com.master.mosaique_capital.entity.Asset;
import com.master.mosaique_capital.entity.AssetTypeEntity;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.enums.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class AssetRepositoryTest {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetTypeRepository assetTypeRepository;

    private User testUser;
    private AssetTypeEntity realEstateType;
    private AssetTypeEntity stockType;

    @BeforeEach
    void setUp() {
        // Nettoyer la base de données
        assetRepository.deleteAll();
        userRepository.deleteAll();

        // Créer un utilisateur de test
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser = userRepository.save(testUser);

        // Récupérer ou créer les types d'actifs
        realEstateType = assetTypeRepository.findByCode(AssetType.REAL_ESTATE.name())
                .orElseGet(() -> {
                    AssetTypeEntity newType = new AssetTypeEntity();
                    newType.setCode(AssetType.REAL_ESTATE.name());
                    newType.setLabel(AssetType.REAL_ESTATE.getLabel());
                    newType.setDescription("Biens immobiliers");
                    return assetTypeRepository.save(newType);
                });

        stockType = assetTypeRepository.findByCode(AssetType.STOCK.name())
                .orElseGet(() -> {
                    AssetTypeEntity newType = new AssetTypeEntity();
                    newType.setCode(AssetType.STOCK.name());
                    newType.setLabel(AssetType.STOCK.getLabel());
                    newType.setDescription("Actions en bourse");
                    return assetTypeRepository.save(newType);
                });

        // Créer des actifs de test
        Asset asset1 = new Asset();
        asset1.setName("Appartement Paris");
        asset1.setDescription("Appartement dans le 15ème arrondissement");
        asset1.setType(realEstateType);
        asset1.setOwner(testUser);
        asset1.setCurrentValue(new BigDecimal("450000"));
        asset1.setCurrency("EUR");
        assetRepository.save(asset1);

        Asset asset2 = new Asset();
        asset2.setName("Actions Amazon");
        asset2.setDescription("Actions Amazon en portefeuille");
        asset2.setType(stockType);
        asset2.setOwner(testUser);
        asset2.setCurrentValue(new BigDecimal("25000"));
        asset2.setCurrency("EUR");
        assetRepository.save(asset2);
    }

    @Test
    void findByOwner_ShouldReturnAllUserAssets() {
        // When
        List<Asset> assets = assetRepository.findByOwner(testUser);

        // Then
        assertEquals(2, assets.size());
        assertTrue(assets.stream().anyMatch(a -> a.getName().equals("Appartement Paris")));
        assertTrue(assets.stream().anyMatch(a -> a.getName().equals("Actions Amazon")));
    }

    @Test
    void findByOwnerAndType_ShouldReturnAssetsOfSpecificType() {
        // When
        List<Asset> realEstateAssets = assetRepository.findByOwnerAndType(testUser, realEstateType);
        List<Asset> stockAssets = assetRepository.findByOwnerAndType(testUser, stockType);

        // Then
        assertEquals(1, realEstateAssets.size());
        assertEquals("Appartement Paris", realEstateAssets.get(0).getName());

        assertEquals(1, stockAssets.size());
        assertEquals("Actions Amazon", stockAssets.get(0).getName());
    }

    @Test
    void sumTotalPatrimony_ShouldReturnCorrectSum() {
        // When
        BigDecimal total = assetRepository.sumTotalPatrimony(testUser);

        // Then
        assertEquals(new BigDecimal("475000"), total);
    }

    @Test
    void getAssetDistributionByType_ShouldReturnCorrectDistribution() {
        // When
        List<AssetRepository.AssetDistributionProjection> distribution =
                assetRepository.getAssetDistributionByType(testUser);

        // Then
        assertEquals(2, distribution.size());

        // Trouver la distribution pour chaque type
        AssetRepository.AssetDistributionProjection realEstateDistribution = distribution.stream()
                .filter(d -> d.getType().equals(AssetType.REAL_ESTATE.name()))
                .findFirst()
                .orElse(null);

        AssetRepository.AssetDistributionProjection stockDistribution = distribution.stream()
                .filter(d -> d.getType().equals(AssetType.STOCK.name()))
                .findFirst()
                .orElse(null);

        assertNotNull(realEstateDistribution);
        assertNotNull(stockDistribution);

        // Vérifier les montants
        assertEquals(new BigDecimal("450000"), realEstateDistribution.getTotal());
        assertEquals(new BigDecimal("25000"), stockDistribution.getTotal());

        // Vérifier les labels
        assertEquals(AssetType.REAL_ESTATE.getLabel(), realEstateDistribution.getLabel());
        assertEquals(AssetType.STOCK.getLabel(), stockDistribution.getLabel());
    }
}