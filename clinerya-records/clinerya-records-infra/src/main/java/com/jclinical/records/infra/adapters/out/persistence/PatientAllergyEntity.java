package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.AllergyCategory;
import com.jclinical.records.domain.model.AllergySeverity;
import com.jclinical.records.domain.model.ClinicalDataSource;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_allergies", schema = "records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientAllergyEntity {

    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "substance", nullable = false)
    private String substance;

    @Column(name = "reaction")
    private String reaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private AllergySeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 16)
    private AllergyCategory category;

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
