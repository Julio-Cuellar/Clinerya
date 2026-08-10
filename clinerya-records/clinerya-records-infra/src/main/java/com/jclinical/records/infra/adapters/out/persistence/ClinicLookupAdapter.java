package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.clinics.domain.model.Clinic;
import com.jclinical.clinics.domain.ports.out.ClinicRepositoryPort;
import com.jclinical.records.domain.ports.out.ClinicLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClinicLookupAdapter implements ClinicLookupPort {

    private final ClinicRepositoryPort clinicRepositoryPort;

    @Override
    public Optional<String> findClinicName(UUID clinicId) {
        return clinicRepositoryPort.findById(clinicId)
                .map(Clinic::getName);
    }
}
