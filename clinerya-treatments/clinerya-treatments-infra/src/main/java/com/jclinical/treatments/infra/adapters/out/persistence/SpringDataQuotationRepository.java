package com.jclinical.treatments.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataQuotationRepository extends JpaRepository<QuotationEntity, UUID> {

    Optional<QuotationEntity> findByIdAndPatientIdAndClinicId(UUID id, UUID patientId, UUID clinicId);

    List<QuotationEntity> findByPatientIdAndClinicIdOrderByCreatedAtDesc(UUID patientId, UUID clinicId);
}
