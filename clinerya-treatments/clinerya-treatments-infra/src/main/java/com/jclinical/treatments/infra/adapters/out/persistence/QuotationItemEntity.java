package com.jclinical.treatments.infra.adapters.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quotation_items", schema = "treatments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationItemEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private QuotationEntity quotation;

    @Column(name = "catalog_item_id")
    private UUID catalogItemId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "tooth_number")
    private Integer toothNumber;

    @Column(name = "labor_charge", nullable = false, precision = 19, scale = 4)
    private BigDecimal laborCharge;

    @Builder.Default
    @OneToMany(mappedBy = "quotationItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<QuotationItemMaterialEntity> materials = new ArrayList<>();

    @Column(name = "discount_percentage")
    private BigDecimal discountPercentage;

    @Column(name = "progress_status", nullable = false)
    private String progressStatus;
}
