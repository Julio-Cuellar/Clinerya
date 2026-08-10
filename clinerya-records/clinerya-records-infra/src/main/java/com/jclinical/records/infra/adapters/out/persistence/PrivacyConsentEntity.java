package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.infra.adapters.out.persistence.security.AesCryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "privacy_consents", schema = "records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyConsentEntity {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "privacy_notice_text", nullable = false, columnDefinition = "TEXT")
    private String privacyNoticeText;

    @Column(name = "document_hash", nullable = false, length = 128)
    private String documentHash;

    @Column(name = "signer_name", nullable = false)
    private String signerName;

    @Column(name = "signature_image", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = AesCryptoConverter.class)
    private String signatureImage;

    @Column(name = "signature_image_hash", nullable = false, length = 128)
    private String signatureImageHash;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "signed_at", nullable = false)
    private LocalDateTime signedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
