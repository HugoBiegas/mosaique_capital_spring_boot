// com/master/mosaique_capital/dto/asset/AssetDto.java
package com.master.mosaique_capital.dto.asset;

import com.master.mosaique_capital.enums.AssetStatus;
import com.master.mosaique_capital.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AssetDto {
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    private String description;

    @NotNull(message = "Le type est obligatoire")
    private AssetType type;

    @NotNull(message = "La valeur actuelle est obligatoire")
    @Positive(message = "La valeur doit être positive")
    private BigDecimal currentValue;

    private String currency = "EUR";

    // ✅ NOUVEAUX CHAMPS POUR LA VENTE
    private AssetStatus status;
    private LocalDate saleDate;
    private BigDecimal salePrice;
    private String saleNotes;
    private BigDecimal purchasePrice;
    private LocalDate purchaseDate;
    private BigDecimal capitalGain;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Méthodes utilitaires
    public boolean isSold() {
        return AssetStatus.SOLD.equals(status);
    }

    public boolean canBeSold() {
        return AssetStatus.ACTIVE.equals(status);
    }
}