package com.jclinical.treatments.infra.adapters.out.persistence;

import com.jclinical.treatments.domain.model.Visit;
import com.jclinical.treatments.domain.ports.out.VisitRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SqlVisitRepository implements VisitRepositoryPort {

    private final SpringDataVisitRepository springRepository;
    private final VisitMapper mapper;

    @Override
    public Visit save(Visit visit) {
        VisitEntity entity = mapper.toEntity(visit);
        VisitEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Visit> findByQuotationIdAndPatientIdAndClinicId(UUID quotationId, UUID patientId, UUID clinicId) {
        return springRepository.findByQuotationIdAndPatientIdAndClinicIdOrderByVisitDateDesc(quotationId, patientId, clinicId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Visit> findByIdAndPatientIdAndClinicId(UUID visitId, UUID patientId, UUID clinicId) {
        return springRepository.findById(visitId)
                .filter(entity -> entity.getPatientId().equals(patientId) && entity.getClinicId().equals(clinicId))
                .map(mapper::toDomain);
    }
}
