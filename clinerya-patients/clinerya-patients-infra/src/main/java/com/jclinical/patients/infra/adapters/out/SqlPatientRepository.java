package com.jclinical.patients.infra.adapters.out;

import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.out.PatientRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlPatientRepository implements PatientRepositoryPort {

    private final SpringDataPatientRepository springDataRepository;
    private final PatientMapper mapper;

    @Override
    public Patient save(Patient patient) {
        PatientEntity entity = mapper.toEntity(patient);
        PatientEntity savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Patient> findByClinicId(UUID clinicId) {
        return springDataRepository.findByClinicId(clinicId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCurpAndClinicId(String curp, UUID clinicId) {
        return springDataRepository.existsByCurpAndClinicId(curp, clinicId);
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }
}
