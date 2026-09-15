package com.jclinical.staff.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataStaffInvitationCompensationRepository
        extends JpaRepository<StaffInvitationCompensationEntity, UUID> {
}
