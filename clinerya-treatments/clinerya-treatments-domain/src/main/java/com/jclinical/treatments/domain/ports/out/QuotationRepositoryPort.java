package com.jclinical.treatments.domain.ports.out;

import com.jclinical.treatments.domain.model.Quotation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuotationRepositoryPort {

    Quotation save(Quotation quotation);

    Optional<Quotation> findByIdAndPatientIdAndClinicId(UUID id, UUID patientId, UUID clinicId);

    List<Quotation> findByPatientIdAndClinicIdOrderByCreatedAtDesc(UUID patientId, UUID clinicId);

    void deleteById(UUID id);
}
