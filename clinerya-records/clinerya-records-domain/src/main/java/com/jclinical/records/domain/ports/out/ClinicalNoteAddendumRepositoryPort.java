package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.ClinicalNoteAddendum;

import java.util.List;
import java.util.UUID;

public interface ClinicalNoteAddendumRepositoryPort {

    ClinicalNoteAddendum save(ClinicalNoteAddendum addendum);

    List<ClinicalNoteAddendum> findByClinicalNoteIdAndClinicIdOrderByCreatedAtAsc(UUID clinicalNoteId, UUID clinicId);
}
