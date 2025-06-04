// com/master/mosaique_capital/entity/Asset.java
package com.master.mosaique_capital.entity;

import com.master.mosaique_capital.enums.AssetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "assets")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    @ToString.Exclude
    private AssetTypeEntity type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User owner;

    @Column(nullable = false)
    private BigDecimal currentValue;

    private String currency = "EUR";

    // ✅ NOUVEAUX CHAMPS POUR LA VENTE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status = AssetStatus.ACTIVE;

    @Column(name = "sale_date")
    private LocalDate saleDate;

    @Column(name = "sale_price")
    private BigDecimal salePrice;

    @Column(name = "sale_notes")
    private String saleNotes;

    // Prix d'acquisition pour calculer la plus-value
    @Column(name = "purchase_price")
    private BigDecimal purchasePrice;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = AssetStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ✅ MÉTHODES MÉTIER POUR LA VENTE

    /**
     * Marque l'actif comme vendu
     */
    public void sell(BigDecimal salePrice, LocalDate saleDate, String notes) {
        if (this.status == AssetStatus.SOLD) {
            throw new IllegalStateException("Cet actif est déjà vendu");
        }

        this.status = AssetStatus.SOLD;
        this.salePrice = salePrice;
        this.saleDate = saleDate != null ? saleDate : LocalDate.now();
        this.saleNotes = notes;
    }

    /**
     * Calcule la plus-value (gain/perte) de la vente
     */
    public BigDecimal calculateCapitalGain() {
        if (status != AssetStatus.SOLD || salePrice == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal costBasis = purchasePrice != null ? purchasePrice : currentValue;
        return salePrice.subtract(costBasis);
    }

    /**
     * Vérifie si l'actif peut être vendu
     */
    public boolean canBeSold() {
        return status == AssetStatus.ACTIVE;
    }

    /**
     * Vérifie si l'actif est vendu
     */
    public boolean isSold() {
        return status == AssetStatus.SOLD;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Asset asset = (Asset) o;
        return getId() != null && Objects.equals(getId(), asset.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}