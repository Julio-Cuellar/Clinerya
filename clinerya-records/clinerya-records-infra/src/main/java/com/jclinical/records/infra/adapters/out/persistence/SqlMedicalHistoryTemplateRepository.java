package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.MedicalHistoryTemplate;
import com.jclinical.records.domain.ports.out.MedicalHistoryTemplateRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlMedicalHistoryTemplateRepository implements MedicalHistoryTemplateRepositoryPort {

    private final SpringDataMedicalHistoryTemplateRepository springRepository;
    private final MedicalHistoryTemplateMapper mapper;

    @Override
    public MedicalHistoryTemplate save(MedicalHistoryTemplate template) {
        MedicalHistoryTemplateEntity entity = mapper.toEntity(template);
        MedicalHistoryTemplateEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<MedicalHistoryTemplate> findByIdAndClinicId(UUID id, UUID clinicId) {
        return springRepository.findByIdAndClinicId(id, clinicId)
                .map(mapper::toDomain);
    }

    @Override
    public List<MedicalHistoryTemplate> findByClinicId(UUID clinicId) {
        return springRepository.findByClinicId(clinicId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return springRepository.existsByIdAndClinicId(id, clinicId);
    }
}
