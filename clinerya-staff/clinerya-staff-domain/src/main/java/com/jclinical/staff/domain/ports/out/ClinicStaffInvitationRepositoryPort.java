package com.jclinical.staff.domain.ports.out;

import com.jclinical.staff.domain.model.ClinicStaffInvitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicStaffInvitationRepositoryPort {

    ClinicStaffInvitation save(ClinicStaffInvitation invitation);

    Optional<ClinicStaffInvitation> findByTokenAndUsedFalse(String token);

    List<ClinicStaffInvitation> findByClinicIdAndUsedFalse(UUID clinicId);

    Optional<ClinicStaffInvitation> findByClinicIdAndEmailAndUsedFalse(UUID clinicId, String email);
}
