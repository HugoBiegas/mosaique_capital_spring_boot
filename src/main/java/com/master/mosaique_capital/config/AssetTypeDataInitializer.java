// com/master/mosaique_capital/config/AssetTypeDataInitializer.java
package com.master.mosaique_capital.config;

import com.master.mosaique_capital.entity.AssetTypeEntity;
import com.master.mosaique_capital.repository.AssetTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Initialisation des types d'assets au démarrage de l'application
 * Cette approche est plus robuste que les scripts SQL car elle évite les problèmes de dépendances
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AssetTypeDataInitializer {

    private final AssetTypeRepository assetTypeRepository;

    @Bean
    public ApplicationRunner initializeAssetTypes() {
        return args -> {
            log.info("🔄 Initialisation des types d'assets...");

            try {
                initializeAssetTypesData();
                log.info("✅ Types d'assets initialisés avec succès");
            } catch (Exception e) {
                log.error("❌ Erreur lors de l'initialisation des types d'assets: {}", e.getMessage(), e);
            }
        };
    }

    @Transactional
    public void initializeAssetTypesData() {
        // 1. Types de base (sans parent)
        AssetTypeEntity realEstate = createOrUpdateAssetType("REAL_ESTATE", "Immobilier", "Biens immobiliers", null);
        AssetTypeEntity financial = createOrUpdateAssetType("FINANCIAL", "Actifs financiers", "Titres, actions, obligations, etc.", null);
        AssetTypeEntity cash = createOrUpdateAssetType("CASH", "Liquidités", "Comptes courants, livrets, etc.", null);
        AssetTypeEntity crypto = createOrUpdateAssetType("CRYPTOCURRENCY", "Cryptomonnaies", "Bitcoin, Ethereum, etc.", null);
        AssetTypeEntity others = createOrUpdateAssetType("OTHERS", "Autres actifs", "Objets de collection, etc.", null);
        AssetTypeEntity liabilities = createOrUpdateAssetType("LIABILITIES", "Passifs", "Dettes et emprunts", null);

        // 2. Sous-types immobiliers
        createOrUpdateAssetType("REAL_ESTATE_RESIDENTIAL", "Résidence principale", "Résidence principale", realEstate);
        createOrUpdateAssetType("REAL_ESTATE_RENTAL", "Immobilier locatif", "Immobilier de rendement", realEstate);
        createOrUpdateAssetType("REAL_ESTATE_COMMERCIAL", "Immobilier commercial", "Locaux professionnels", realEstate);
        createOrUpdateAssetType("REAL_ESTATE_LAND", "Terrains", "Terrains non bâtis", realEstate);
        createOrUpdateAssetType("REAL_ESTATE_SCPI", "SCPI", "Sociétés civiles de placement immobilier", realEstate);

        // 3. Sous-types financiers
        createOrUpdateAssetType("FINANCIAL_STOCKS", "Actions", "Titres de propriété d'entreprises", financial);
        createOrUpdateAssetType("FINANCIAL_BONDS", "Obligations", "Titres de créance", financial);
        createOrUpdateAssetType("FINANCIAL_FUNDS", "Fonds", "OPCVM, ETF, etc.", financial);
        createOrUpdateAssetType("FINANCIAL_LIFE_INSURANCE", "Assurance-vie", "Contrats d'assurance-vie", financial);
        createOrUpdateAssetType("FINANCIAL_PEA", "PEA", "Plan d'Épargne en Actions", financial);
        createOrUpdateAssetType("FINANCIAL_RETIREMENT", "Épargne retraite", "PER, PERP, Madelin, etc.", financial);

        // 4. Sous-types liquidités
        createOrUpdateAssetType("CASH_CURRENT", "Compte courant", "Comptes à vue", cash);
        createOrUpdateAssetType("CASH_SAVINGS", "Livrets", "Livrets réglementés et livrets bancaires", cash);
        createOrUpdateAssetType("CASH_TERM", "Dépôts à terme", "Comptes à terme", cash);

        // 5. Sous-types autres actifs
        createOrUpdateAssetType("OTHERS_ART", "Art et collections", "Œuvres d'art, collections diverses", others);
        createOrUpdateAssetType("OTHERS_PRECIOUS", "Métaux précieux", "Or, argent, etc.", others);
        createOrUpdateAssetType("OTHERS_VEHICLES", "Véhicules", "Voitures, bateaux, etc.", others);

        // 6. Sous-types passifs
        createOrUpdateAssetType("LIABILITIES_MORTGAGE", "Crédit immobilier", "Emprunts pour l'immobilier", liabilities);
        createOrUpdateAssetType("LIABILITIES_CONSUMER", "Crédit à la consommation", "Emprunts pour des biens de consommation", liabilities);
        createOrUpdateAssetType("LIABILITIES_STUDENT", "Prêt étudiant", "Emprunts pour les études", liabilities);
        createOrUpdateAssetType("LIABILITIES_OTHER", "Autres dettes", "Autres types de dettes", liabilities);

        log.info("📊 Nombre total de types d'assets en base: {}", assetTypeRepository.count());
    }

    /**
     * Crée ou met à jour un type d'asset
     * Cette méthode est idempotente et évite les doublons
     */
    private AssetTypeEntity createOrUpdateAssetType(String code, String label, String description, AssetTypeEntity parent) {
        Optional<AssetTypeEntity> existingType = assetTypeRepository.findByCode(code);

        if (existingType.isPresent()) {
            AssetTypeEntity assetType = existingType.get();
            // Mise à jour si nécessaire
            if (!label.equals(assetType.getLabel()) || !description.equals(assetType.getDescription())) {
                assetType.setLabel(label);
                assetType.setDescription(description);
                assetType.setParentType(parent);
                assetType = assetTypeRepository.save(assetType);
                log.debug("🔄 Type d'asset mis à jour: {}", code);
            }
            return assetType;
        } else {
            // Création d'un nouveau type
            AssetTypeEntity newType = new AssetTypeEntity();
            newType.setCode(code);
            newType.setLabel(label);
            newType.setDescription(description);
            newType.setParentType(parent);

            AssetTypeEntity savedType = assetTypeRepository.save(newType);
            log.debug("✅ Nouveau type d'asset créé: {}", code);
            return savedType;
        }
    }

    /**
     * Méthode utilitaire pour vérifier tous les types requis
     */
    public void validateAllAssetTypes() {
        List<String> requiredTypes = Arrays.asList(
                "REAL_ESTATE", "REAL_ESTATE_RESIDENTIAL", "REAL_ESTATE_RENTAL", "REAL_ESTATE_COMMERCIAL", "REAL_ESTATE_LAND", "REAL_ESTATE_SCPI",
                "FINANCIAL", "FINANCIAL_STOCKS", "FINANCIAL_BONDS", "FINANCIAL_FUNDS", "FINANCIAL_LIFE_INSURANCE", "FINANCIAL_PEA", "FINANCIAL_RETIREMENT",
                "CASH", "CASH_CURRENT", "CASH_SAVINGS", "CASH_TERM",
                "CRYPTOCURRENCY",
                "OTHERS", "OTHERS_ART", "OTHERS_PRECIOUS", "OTHERS_VEHICLES",
                "LIABILITIES", "LIABILITIES_MORTGAGE", "LIABILITIES_CONSUMER", "LIABILITIES_STUDENT", "LIABILITIES_OTHER"
        );

        List<String> missingTypes = requiredTypes.stream()
                .filter(type -> assetTypeRepository.findByCode(type).isEmpty())
                .toList();

        if (!missingTypes.isEmpty()) {
            log.warn("⚠️ Types d'assets manquants: {}", missingTypes);
        } else {
            log.info("✅ Tous les types d'assets requis sont présents");
        }
    }
}