package com.jclinical.treatments.infra.adapters.out.persistence;

import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.ports.out.QuotationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlQuotationRepository implements QuotationRepositoryPort {

    private final SpringDataQuotationRepository springRepository;
    private final QuotationMapper mapper;

    @Override
    public Quotation save(Quotation quotation) {
        QuotationEntity entity = mapper.toEntity(quotation);
        // MapStruct no puede resolver la referencia inversa item -> quotation (no existe en el dominio),
        // así que se cablea manualmente antes de persistir para que Hibernate mantenga la relación.
        entity.getItems().forEach(item -> item.setQuotation(entity));
        QuotationEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Quotation> findByIdAndPatientIdAndClinicId(UUID id, UUID patientId, UUID clinicId) {
        return springRepository.findByIdAndPatientIdAndClinicId(id, patientId, clinicId).map(mapper::toDomain);
    }

    @Override
    public List<Quotation> findByPatientIdAndClinicIdOrderByCreatedAtDesc(UUID patientId, UUID clinicId) {
        return springRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springRepository.deleteById(id);
    }
}
