package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.ClinicalDataSource;
import com.jclinical.records.domain.model.ConditionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_conditions", schema = "records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientConditionEntity {

    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "icd10_code", length = 16)
    private String icd10Code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ConditionStatus status;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private ClinicalDataSource source;

    @Column(name = "noted_by_user_id")
    private UUID notedByUserId;

    @Column(name = "noted_by_user_name")
    private String notedByUserName;

    @Column(name = "noted_at", nullable = false)
    private LocalDateTime notedAt;
}
