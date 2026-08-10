package com.jclinical.records.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataDocumentSignatureRepository extends JpaRepository<DocumentSignatureEntity, UUID> {

    @Modifying
    @Query("""
            update DocumentSignatureEntity signature
               set signature.status = 'SUPERSEDED'
             where signature.documentType = :documentType
               and signature.documentId = :documentId
               and signature.signatureFieldId = :signatureFieldId
               and signature.status = 'ACTIVE'
            """)
    void supersedeActiveSignatures(String documentType, UUID documentId, String signatureFieldId);
}
