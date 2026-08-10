package com.jclinical.treatments.infra.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "treatment_catalog_materials", schema = "treatments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentCatalogMaterialEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private TreatmentCatalogItemEntity catalogItem;

    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @Column(name = "material_name", nullable = false)
    private String materialName;

    @Column(name = "typical_quantity", precision = 19, scale = 4)
    private BigDecimal typicalQuantity;
}
