package com.jclinical.integrations.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataStaffCalendarCredentialsRepository extends JpaRepository<StaffCalendarCredentialsEntity, UUID> {

    Optional<StaffCalendarCredentialsEntity> findByClinicIdAndStaffId(UUID clinicId, UUID staffId);

    @Transactional
    void deleteByClinicIdAndStaffId(UUID clinicId, UUID staffId);
}
