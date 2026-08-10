package com.jclinical.records.infra.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.jclinical.records.infra.adapters.out.persistence.security.AesCryptoConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medical_histories", schema = "records", uniqueConstraints = @UniqueConstraint(columnNames = { "patient_id",
        "template_id", "clinic_id" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "answers_json", columnDefinition = "TEXT", nullable = false)
    @Convert(converter = AesCryptoConverter.class)
    private String answersJson;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
