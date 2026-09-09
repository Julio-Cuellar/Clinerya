package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.ClinicalDataSource;
import com.jclinical.records.domain.model.PatientAllergy;
import com.jclinical.records.domain.ports.out.PatientAllergyRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlPatientAllergyRepository implements PatientAllergyRepositoryPort {

    private final SpringDataPatientAllergyRepository repository;

    @Override
    public PatientAllergy save(PatientAllergy allergy) {
        return toDomain(repository.save(toEntity(allergy)));
    }

    @Override
    public List<PatientAllergy> findByClinicIdAndPatientId(UUID clinicId, UUID patientId) {
        return repository.findByClinicIdAndPatientIdOrderByNotedAtDesc(clinicId, patientId).stream()
                .map(SqlPatientAllergyRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<PatientAllergy> findByIdAndClinicId(UUID id, UUID clinicId) {
        return repository.findByIdAndClinicId(id, clinicId).map(SqlPatientAllergyRepository::toDomain);
    }

    @Override
    @Transactional
    public void deleteByIdAndClinicId(UUID id, UUID clinicId) {
        repository.deleteByIdAndClinicId(id, clinicId);
    }

    @Override
    @Transactional
    public void deleteByClinicIdAndPatientIdAndSource(UUID clinicId, UUID patientId, ClinicalDataSource source) {
        repository.deleteByClinicIdAndPatientIdAndSource(clinicId, patientId, source);
    }

    private static PatientAllergyEntity toEntity(PatientAllergy domain) {
        return PatientAllergyEntity.builder()
                .id(domain.getId())
                .clinicId(domain.getClinicId())
                .patientId(domain.getPatientId())
                .substance(domain.getSubstance())
                .reaction(domain.getReaction())
                .severity(domain.getSeverity())
                .category(domain.getCategory())
                .source(domain.getSource())
                .notedByUserId(domain.getNotedByUserId())
                .notedByUserName(domain.getNotedByUserName())
                .notedAt(domain.getNotedAt())
                .build();
    }

    private static PatientAllergy toDomain(PatientAllergyEntity entity) {
        return PatientAllergy.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .patientId(entity.getPatientId())
                .substance(entity.getSubstance())
                .reaction(entity.getReaction())
                .severity(entity.getSeverity())
                .category(entity.getCategory())
                .source(entity.getSource())
                .notedByUserId(entity.getNotedByUserId())
                .notedByUserName(entity.getNotedByUserName())
                .notedAt(entity.getNotedAt())
                .build();
    }
}
