// com/master/mosaique_capital/dto/asset/AssetValuationDto.java
package com.master.mosaique_capital.dto.asset;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AssetValuationDto {
    private Long id;

    private Long assetId;

    @NotNull(message = "La date de valorisation est obligatoire")
    private LocalDate valuationDate;

    @NotNull(message = "La valeur est obligatoire")
    @Positive(message = "La valeur doit être positive")
    private BigDecimal value;

    private String currency = "EUR";

    private String source;

    private LocalDateTime createdAt;
}