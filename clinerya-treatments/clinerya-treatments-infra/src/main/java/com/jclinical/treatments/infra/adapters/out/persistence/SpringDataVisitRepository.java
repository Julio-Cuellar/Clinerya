package com.jclinical.treatments.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataVisitRepository extends JpaRepository<VisitEntity, UUID> {

    List<VisitEntity> findByQuotationIdAndPatientIdAndClinicIdOrderByVisitDateDesc(UUID quotationId, UUID patientId, UUID clinicId);
}
