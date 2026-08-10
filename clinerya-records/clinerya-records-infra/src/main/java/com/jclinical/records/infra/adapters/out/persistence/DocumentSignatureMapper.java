package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.DocumentSignature;

public interface DocumentSignatureMapper {
    DocumentSignatureEntity toEntity(DocumentSignature domain);

    DocumentSignature toDomain(DocumentSignatureEntity entity);
}
