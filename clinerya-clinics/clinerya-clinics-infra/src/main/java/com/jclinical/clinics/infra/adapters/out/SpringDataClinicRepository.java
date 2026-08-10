package com.jclinical.clinics.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataClinicRepository extends JpaRepository<ClinicEntity, UUID> {
    List<ClinicEntity> findByOwnerUserId(UUID ownerUserId);
}
