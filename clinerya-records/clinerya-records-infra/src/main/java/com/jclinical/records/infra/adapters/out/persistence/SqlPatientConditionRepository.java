package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.ClinicalDataSource;
import com.jclinical.records.domain.model.PatientCondition;
import com.jclinical.records.domain.ports.out.PatientConditionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlPatientConditionRepository implements PatientConditionRepositoryPort {

    private final SpringDataPatientConditionRepository repository;

    @Override
    public PatientCondition save(PatientCondition condition) {
        return toDomain(repository.save(toEntity(condition)));
    }

    @Override
    public List<PatientCondition> findByClinicIdAndPatientId(UUID clinicId, UUID patientId) {
        return repository.findByClinicIdAndPatientIdOrderByNotedAtDesc(clinicId, patientId).stream()
                .map(SqlPatientConditionRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<PatientCondition> findByIdAndClinicId(UUID id, UUID clinicId) {
        return repository.findByIdAndClinicId(id, clinicId).map(SqlPatientConditionRepository::toDomain);
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

    private static PatientConditionEntity toEntity(PatientCondition domain) {
        return PatientConditionEntity.builder()
                .id(domain.getId())
                .clinicId(domain.getClinicId())
                .patientId(domain.getPatientId())
                .name(domain.getName())
                .icd10Code(domain.getIcd10Code())
                .status(domain.getStatus())
                .onsetDate(domain.getOnsetDate())
                .source(domain.getSource())
                .notedByUserId(domain.getNotedByUserId())
                .notedByUserName(domain.getNotedByUserName())
                .notedAt(domain.getNotedAt())
                .build();
    }

    private static PatientCondition toDomain(PatientConditionEntity entity) {
        return PatientCondition.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .patientId(entity.getPatientId())
                .name(entity.getName())
                .icd10Code(entity.getIcd10Code())
                .status(entity.getStatus())
                .onsetDate(entity.getOnsetDate())
                .source(entity.getSource())
                .notedByUserId(entity.getNotedByUserId())
                .notedByUserName(entity.getNotedByUserName())
                .notedAt(entity.getNotedAt())
                .build();
    }
}
