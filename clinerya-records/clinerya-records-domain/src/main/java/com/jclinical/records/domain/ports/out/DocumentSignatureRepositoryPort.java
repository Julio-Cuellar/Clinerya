package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.DocumentSignature;
import com.jclinical.records.domain.model.SignatureDocumentType;

import java.util.UUID;

public interface DocumentSignatureRepositoryPort {
    DocumentSignature save(DocumentSignature signature);

    void supersedeActiveSignatures(SignatureDocumentType documentType, UUID documentId, String signatureFieldId);
}
