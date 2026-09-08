package com.jclinical.staff.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataStaffCompensationRepository extends JpaRepository<StaffCompensationEntity, UUID> {

    List<StaffCompensationEntity> findByClinicId(UUID clinicId);
}
