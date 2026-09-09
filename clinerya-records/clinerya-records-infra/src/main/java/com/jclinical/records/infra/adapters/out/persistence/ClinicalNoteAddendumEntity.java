package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.infra.adapters.out.persistence.security.AesCryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
@Table(name = "clinical_note_addenda", schema = "records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalNoteAddendumEntity {

    @Id
    private UUID id;

    @Column(name = "clinical_note_id", nullable = false)
    private UUID clinicalNoteId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Convert(converter = AesCryptoConverter.class)
    private String content;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_by_user_name", nullable = false)
    private String createdByUserName;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
