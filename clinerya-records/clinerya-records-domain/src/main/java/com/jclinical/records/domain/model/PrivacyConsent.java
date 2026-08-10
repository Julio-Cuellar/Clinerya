package com.jclinical.records.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyConsent {
    private UUID id;
    private UUID patientId;
    private UUID clinicId;
    private String privacyNoticeText;
    private String documentHash;
    private String signerName;
    private String signatureImage;
    private String signatureImageHash;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;
}
