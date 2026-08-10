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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_signatures", schema = "records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSignatureEntity {

    @Id
    private UUID id;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "signer_type", nullable = false)
    private String signerType;

    @Column(name = "signer_user_id")
    private UUID signerUserId;

    @Column(name = "signer_name", nullable = false)
    private String signerName;

    @Column(name = "signature_field_id")
    private String signatureFieldId;

    @Column(name = "signature_field_label")
    private String signatureFieldLabel;

    @Column(name = "signature_image_hash")
    private String signatureImageHash;

    @Column(name = "document_hash", nullable = false)
    private String documentHash;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(nullable = false)
    private String status;

    @Column(name = "signed_at", nullable = false)
    private LocalDateTime signedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
