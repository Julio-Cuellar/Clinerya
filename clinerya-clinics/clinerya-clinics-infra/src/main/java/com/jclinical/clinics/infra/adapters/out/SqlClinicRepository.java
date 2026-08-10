package com.jclinical.clinics.infra.adapters.out;

import com.jclinical.clinics.domain.model.Clinic;
import com.jclinical.clinics.domain.ports.out.ClinicRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SqlClinicRepository implements ClinicRepositoryPort {

    private final SpringDataClinicRepository springDataRepository;
    private final ClinicMapper mapper;

    @Override
    public Clinic save(Clinic clinic) {
        ClinicEntity entity = mapper.toEntity(clinic);
        ClinicEntity savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Clinic> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Clinic> findByOwnerUserId(UUID ownerUserId) {
        return springDataRepository.findByOwnerUserId(ownerUserId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
