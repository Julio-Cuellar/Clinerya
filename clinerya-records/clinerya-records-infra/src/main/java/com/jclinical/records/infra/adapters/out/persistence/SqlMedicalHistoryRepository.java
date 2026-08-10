package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.MedicalHistory;
import com.jclinical.records.domain.ports.out.MedicalHistoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlMedicalHistoryRepository implements MedicalHistoryRepositoryPort {

    private final SpringDataMedicalHistoryRepository springRepository;
    private final MedicalHistoryMapper mapper;

    @Override
    public Optional<MedicalHistory> findByPatientIdAndTemplateIdAndClinicId(UUID patientId, UUID templateId, UUID clinicId) {
        return springRepository.findByPatientIdAndTemplateIdAndClinicId(patientId, templateId, clinicId)
                .map(mapper::toDomain);
    }

    @Override
    public List<MedicalHistory> findByPatientIdAndClinicId(UUID patientId, UUID clinicId) {
        return springRepository.findByPatientIdAndClinicId(patientId, clinicId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public MedicalHistory save(MedicalHistory medicalHistory) {
        MedicalHistoryEntity entity = mapper.toEntity(medicalHistory);
        MedicalHistoryEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
