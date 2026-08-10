package com.jclinical.treatments.domain.ports.out;

import com.jclinical.treatments.domain.model.Visit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitRepositoryPort {

    Visit save(Visit visit);

    List<Visit> findByQuotationIdAndPatientIdAndClinicId(UUID quotationId, UUID patientId, UUID clinicId);

    Optional<Visit> findByIdAndPatientIdAndClinicId(UUID visitId, UUID patientId, UUID clinicId);
}
