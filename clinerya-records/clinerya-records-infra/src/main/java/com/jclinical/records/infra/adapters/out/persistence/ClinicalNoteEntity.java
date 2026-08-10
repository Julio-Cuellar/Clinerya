package com.jclinical.records.infra.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.jclinical.records.infra.adapters.out.persistence.security.AesCryptoConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clinical_notes", schema = "records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalNoteEntity {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = AesCryptoConverter.class)
    private String subjective;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = AesCryptoConverter.class)
    private String objective;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = AesCryptoConverter.class)
    private String assessment;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = AesCryptoConverter.class)
    private String plan;

    @Column(nullable = false)
    private String status;

    @Column(name = "authored_by_external_user_id")
    private UUID authoredByExternalUserId;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signed_by_user_id")
    private UUID signedByUserId;

    @Column(name = "document_hash")
    private String documentHash;

    // Signos Vitales (Vital Signs)
    @Column(name = "vital_temp")
    private Double vitalTemp;

    @Column(name = "vital_bp")
    private String vitalBp;

    @Column(name = "vital_hr")
    private Integer vitalHr;

    @Column(name = "vital_rr")
    private Integer vitalRr;

    @Column(name = "vital_weight")
    private Double vitalWeight;

    @Column(name = "vital_height")
    private Double vitalHeight;

    @Column(name = "vital_bmi")
    private Double vitalBmi;

    @Column(name = "vital_o2")
    private Integer vitalO2;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
