package com.jclinical.staff.domain.ports.out;

import com.jclinical.staff.domain.model.StaffInvitationCompensation;

import java.util.Optional;
import java.util.UUID;

public interface StaffInvitationCompensationRepositoryPort {

    StaffInvitationCompensation save(StaffInvitationCompensation compensation);

    Optional<StaffInvitationCompensation> findByInvitationId(UUID invitationId);

    void deleteByInvitationId(UUID invitationId);
}
