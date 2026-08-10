package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.DocumentSignature;
import com.jclinical.records.domain.model.SignatureDocumentType;
import com.jclinical.records.domain.ports.out.DocumentSignatureRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlDocumentSignatureRepository implements DocumentSignatureRepositoryPort {

    private final SpringDataDocumentSignatureRepository springRepository;
    private final DocumentSignatureMapper mapper;

    @Override
    public DocumentSignature save(DocumentSignature signature) {
        DocumentSignatureEntity entity = mapper.toEntity(signature);
        return mapper.toDomain(springRepository.save(entity));
    }

    @Override
    public void supersedeActiveSignatures(SignatureDocumentType documentType, UUID documentId, String signatureFieldId) {
        if (signatureFieldId == null || signatureFieldId.isBlank()) {
            return;
        }
        springRepository.supersedeActiveSignatures(documentType.name(), documentId, signatureFieldId);
    }
}
