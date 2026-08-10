package com.jclinical.accounting.infra.adapters.out.persistence;

import com.jclinical.accounting.domain.model.OpeningBalanceSetup;
import com.jclinical.accounting.domain.ports.out.OpeningBalanceSetupRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlOpeningBalanceSetupRepository implements OpeningBalanceSetupRepositoryPort {

    private final SpringDataOpeningBalanceSetupRepository springRepository;
    private final OpeningBalanceSetupMapper mapper;

    @Override
    public OpeningBalanceSetup save(OpeningBalanceSetup setup) {
        OpeningBalanceSetupEntity saved = springRepository.save(mapper.toEntity(setup));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<OpeningBalanceSetup> findByClinicId(UUID clinicId) {
        return springRepository.findByClinicId(clinicId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByClinicId(UUID clinicId) {
        return springRepository.existsByClinicId(clinicId);
    }
}
