// com/master/mosaique_capital/enums/AssetType.java
package com.master.mosaique_capital.enums;

import lombok.Getter;

@Getter
public enum AssetType {
    REAL_ESTATE("Immobilier"),
    STOCK("Actions"),
    BOND("Obligations"),
    BANK_ACCOUNT("Compte bancaire"),
    LIFE_INSURANCE("Assurance-vie"),
    INVESTMENT_FUND("Fonds d'investissement"),
    CRYPTOCURRENCY("Cryptomonnaie"),
    OTHER("Autre");

    private final String label;

    AssetType(String label) {
        this.label = label;
    }

}