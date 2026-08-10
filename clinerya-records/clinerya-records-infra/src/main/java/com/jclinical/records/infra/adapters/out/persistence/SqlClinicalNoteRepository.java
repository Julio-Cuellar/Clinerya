package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlClinicalNoteRepository implements ClinicalNoteRepositoryPort {

    private final SpringDataClinicalNoteRepository springRepository;
    private final ClinicalNoteMapper mapper;

    @Override
    public Optional<ClinicalNote> findByIdAndPatientIdAndClinicId(UUID id, UUID patientId, UUID clinicId) {
        return springRepository.findByIdAndPatientIdAndClinicId(id, patientId, clinicId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ClinicalNote> findByPatientIdAndClinicIdOrderByCreatedAtDesc(UUID patientId, UUID clinicId) {
        return springRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ClinicalNote save(ClinicalNote clinicalNote) {
        ClinicalNoteEntity entity = mapper.toEntity(clinicalNote);
        ClinicalNoteEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
