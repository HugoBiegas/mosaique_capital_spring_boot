// com/master/mosaique_capital/enums/AssetStatus.java
package com.master.mosaique_capital.enums;

import lombok.Getter;

/**
 * Statuts possibles d'un actif dans le portefeuille
 */
@Getter
public enum AssetStatus {
    /**
     * Actif actif dans le portefeuille
     */
    ACTIVE("Actif", "L'actif est présent dans le portefeuille"),

    /**
     * Actif vendu
     */
    SOLD("Vendu", "L'actif a été vendu et n'est plus dans le portefeuille"),

    /**
     * Actif en cours de vente
     */
    PENDING_SALE("Vente en cours", "L'actif est en cours de vente"),

    /**
     * Actif suspendu/gelé
     */
    SUSPENDED("Suspendu", "L'actif est temporairement suspendu");

    private final String label;
    private final String description;

    AssetStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }
}