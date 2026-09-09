package com.jclinical.records.infra.adapters.out.persistence;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_medications", schema = "records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientMedicationEntity {

    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "medication_name", nullable = false)
    private String medicationName;

    @Column(name = "dose", length = 128)
    private String dose;

    @Column(name = "schedule", length = 128)
    private String schedule;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "started_on")
    private LocalDate startedOn;

    @Column(name = "stopped_on")
    private LocalDate stoppedOn;

    @Column(name = "prescription_id")
    private UUID prescriptionId;

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
