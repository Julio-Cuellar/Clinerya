package com.jclinical.staff.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataDoctorProfileRepository extends JpaRepository<DoctorProfileEntity, UUID> {
    Optional<DoctorProfileEntity> findByClinicStaffId(UUID clinicStaffId);
}
