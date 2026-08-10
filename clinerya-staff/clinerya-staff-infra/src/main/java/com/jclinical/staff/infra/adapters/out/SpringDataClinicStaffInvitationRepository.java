package com.jclinical.staff.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataClinicStaffInvitationRepository extends JpaRepository<ClinicStaffInvitationEntity, UUID> {

    Optional<ClinicStaffInvitationEntity> findByTokenAndUsedFalse(String token);

    List<ClinicStaffInvitationEntity> findByClinicIdAndUsedFalse(UUID clinicId);

    Optional<ClinicStaffInvitationEntity> findByClinicIdAndEmailAndUsedFalse(UUID clinicId, String email);
}
