// com/master/mosaique_capital/enums/AssetType.java
package com.master.mosaique_capital.enums;

import lombok.Getter;

@Getter
public enum AssetType {
    // Immobilier
    REAL_ESTATE("Immobilier"),
    REAL_ESTATE_RESIDENTIAL("Résidence principale"),
    REAL_ESTATE_RENTAL("Immobilier locatif"),
    REAL_ESTATE_COMMERCIAL("Immobilier commercial"),
    REAL_ESTATE_LAND("Terrains"),
    REAL_ESTATE_SCPI("SCPI"),

    // Actifs financiers
    FINANCIAL("Actifs financiers"),
    FINANCIAL_STOCKS("Actions"),
    FINANCIAL_BONDS("Obligations"),
    FINANCIAL_FUNDS("Fonds"),
    FINANCIAL_LIFE_INSURANCE("Assurance-vie"),
    FINANCIAL_PEA("PEA"),
    FINANCIAL_RETIREMENT("Épargne retraite"),

    // Liquidités
    CASH("Liquidités"),
    CASH_CURRENT("Compte courant"),
    CASH_SAVINGS("Livrets"),
    CASH_TERM("Dépôts à terme"),

    // Cryptomonnaies
    CRYPTOCURRENCY("Cryptomonnaies"),

    // Autres actifs
    OTHERS("Autres actifs"),
    OTHERS_ART("Art et collections"),
    OTHERS_PRECIOUS("Métaux précieux"),
    OTHERS_VEHICLES("Véhicules"),

    // Passifs
    LIABILITIES("Passifs"),
    LIABILITIES_MORTGAGE("Crédit immobilier"),
    LIABILITIES_CONSUMER("Crédit à la consommation"),
    LIABILITIES_STUDENT("Prêt étudiant"),
    LIABILITIES_OTHER("Autres dettes");

    private final String label;

    AssetType(String label) {
        this.label = label;
    }
}