package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.ports.out.StaffInvitationNotifierPort;
import com.jclinical.users.domain.ports.out.EmailSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EmailStaffInvitationNotifier implements StaffInvitationNotifierPort {

    private final EmailSenderPort emailSender;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Override
    public void sendInvitation(String email, StaffRole role, String token, LocalDateTime expiresAt) {
        String invitationUrl = frontendBaseUrl.replaceFirst("/+$", "")
                + "/confirm-staff?token=" + token;
        emailSender.sendStaffInvitation(email, role.name(), invitationUrl, expiresAt);
    }
}
