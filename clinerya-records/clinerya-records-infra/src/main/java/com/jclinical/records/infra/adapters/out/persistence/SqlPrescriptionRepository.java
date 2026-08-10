package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.Prescription;
import com.jclinical.records.domain.ports.out.PrescriptionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SqlPrescriptionRepository implements PrescriptionRepositoryPort {

    private final SpringDataPrescriptionRepository springDataRepository;
    private final PrescriptionMapper mapper;

    public SqlPrescriptionRepository(SpringDataPrescriptionRepository springDataRepository, PrescriptionMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Prescription save(Prescription prescription) {
        PrescriptionEntity entity = mapper.toEntity(prescription);
        PrescriptionEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Prescription> findByClinicIdAndPatientId(UUID clinicId, UUID patientId) {
        return springDataRepository.findByClinicIdAndPatientIdOrderByCreatedAtDesc(clinicId, patientId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Prescription> findById(UUID clinicId, UUID prescriptionId) {
        return springDataRepository.findByClinicIdAndId(clinicId, prescriptionId)
                .map(mapper::toDomain);
    }
}
