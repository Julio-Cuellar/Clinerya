package com.jclinical.staff.domain.ports.out;

import com.jclinical.staff.domain.model.StaffRole;

import java.time.LocalDateTime;

public interface StaffInvitationNotifierPort {

    void sendInvitation(String email, StaffRole role, String token, LocalDateTime expiresAt);
}
