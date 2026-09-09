package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.PatientMedication;
import com.jclinical.records.domain.ports.out.PatientMedicationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlPatientMedicationRepository implements PatientMedicationRepositoryPort {

    private final SpringDataPatientMedicationRepository repository;

    @Override
    public PatientMedication save(PatientMedication medication) {
        return toDomain(repository.save(toEntity(medication)));
    }

    @Override
    public List<PatientMedication> findByClinicIdAndPatientId(UUID clinicId, UUID patientId) {
        return repository.findByClinicIdAndPatientIdOrderByNotedAtDesc(clinicId, patientId).stream()
                .map(SqlPatientMedicationRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<PatientMedication> findByIdAndClinicId(UUID id, UUID clinicId) {
        return repository.findByIdAndClinicId(id, clinicId).map(SqlPatientMedicationRepository::toDomain);
    }

    @Override
    @Transactional
    public void deleteByIdAndClinicId(UUID id, UUID clinicId) {
        repository.deleteByIdAndClinicId(id, clinicId);
    }

    private static PatientMedicationEntity toEntity(PatientMedication domain) {
        return PatientMedicationEntity.builder()
                .id(domain.getId())
                .clinicId(domain.getClinicId())
                .patientId(domain.getPatientId())
                .medicationName(domain.getMedicationName())
                .dose(domain.getDose())
                .schedule(domain.getSchedule())
                .active(domain.isActive())
                .startedOn(domain.getStartedOn())
                .stoppedOn(domain.getStoppedOn())
                .prescriptionId(domain.getPrescriptionId())
                .source(domain.getSource())
                .notedByUserId(domain.getNotedByUserId())
                .notedByUserName(domain.getNotedByUserName())
                .notedAt(domain.getNotedAt())
                .build();
    }

    private static PatientMedication toDomain(PatientMedicationEntity entity) {
        return PatientMedication.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .patientId(entity.getPatientId())
                .medicationName(entity.getMedicationName())
                .dose(entity.getDose())
                .schedule(entity.getSchedule())
                .active(entity.isActive())
                .startedOn(entity.getStartedOn())
                .stoppedOn(entity.getStoppedOn())
                .prescriptionId(entity.getPrescriptionId())
                .source(entity.getSource())
                .notedByUserId(entity.getNotedByUserId())
                .notedByUserName(entity.getNotedByUserName())
                .notedAt(entity.getNotedAt())
                .build();
    }
}
