package com.jclinical.treatments.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataTreatmentCatalogRepository extends JpaRepository<TreatmentCatalogItemEntity, UUID> {

    Optional<TreatmentCatalogItemEntity> findByIdAndClinicId(UUID id, UUID clinicId);

    List<TreatmentCatalogItemEntity> findByClinicId(UUID clinicId);

    List<TreatmentCatalogItemEntity> findByClinicIdAndActiveTrue(UUID clinicId);
}
