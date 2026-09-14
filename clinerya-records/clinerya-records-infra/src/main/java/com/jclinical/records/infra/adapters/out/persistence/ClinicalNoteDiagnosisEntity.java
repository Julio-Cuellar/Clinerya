package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.DiagnosisKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clinical_note_diagnoses", schema = "records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalNoteDiagnosisEntity {

    @Id
    private UUID id;

    @Column(name = "clinical_note_id", nullable = false)
    private UUID clinicalNoteId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "icd10_code", nullable = false, length = 10)
    private String icd10Code;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private DiagnosisKind kind;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
