package com.jclinical.records.infra.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "icd10_catalog", schema = "records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Icd10CodeEntity {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 255)
    private String chapter;

    @Column(nullable = false)
    private boolean billable;
}
