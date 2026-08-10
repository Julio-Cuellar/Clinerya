package com.jclinical.records.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSignature {
    private UUID id;
    private SignatureDocumentType documentType;
    private UUID documentId;
    private UUID clinicId;
    private UUID patientId;
    private SignerType signerType;
    private UUID signerUserId;
    private String signerName;
    private String signatureFieldId;
    private String signatureFieldLabel;
    private String signatureImageHash;
    private String documentHash;
    private String ipAddress;
    private String userAgent;
    private DocumentSignatureStatus status;
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;
}
