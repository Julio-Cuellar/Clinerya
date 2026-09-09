package com.jclinical.records.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataClinicalNoteAddendumRepository extends JpaRepository<ClinicalNoteAddendumEntity, UUID> {

    List<ClinicalNoteAddendumEntity> findByClinicalNoteIdAndClinicIdOrderByCreatedAtAsc(UUID clinicalNoteId, UUID clinicId);
}
