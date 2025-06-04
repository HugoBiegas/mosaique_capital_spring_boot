// com/master/mosaique_capital/dto/asset/AssetSaleResponse.java
package com.master.mosaique_capital.dto.asset;

import com.master.mosaique_capital.enums.AssetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de réponse après vente d'un actif
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSaleResponse {

    private Long assetId;
    private String assetName;
    private AssetStatus status;
    private BigDecimal salePrice;
    private LocalDate saleDate;
    private String saleNotes;
    private BigDecimal capitalGain;
    private BigDecimal capitalGainPercentage;
    private LocalDateTime soldAt;

    /**
     * Calcule le pourcentage de plus-value
     */
    public void calculateCapitalGainPercentage(BigDecimal purchasePrice) {
        if (purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) > 0 && salePrice != null) {
            BigDecimal gain = salePrice.subtract(purchasePrice);
            this.capitalGainPercentage = gain
                    .divide(purchasePrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }
}
