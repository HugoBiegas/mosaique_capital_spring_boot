// com/master/mosaique_capital/dto/asset/AssetSaleRequest.java
package com.master.mosaique_capital.dto.asset;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour les demandes de vente d'actifs
 */
@Data
public class AssetSaleRequest {

    @NotNull(message = "Le prix de vente est obligatoire")
    @Positive(message = "Le prix de vente doit être positif")
    private BigDecimal salePrice;

    private LocalDate saleDate; // Si null, utilise la date actuelle

    private String saleNotes;

    private String currency = "EUR";
}
