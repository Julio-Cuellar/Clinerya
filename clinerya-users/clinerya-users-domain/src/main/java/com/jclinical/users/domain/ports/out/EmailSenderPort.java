package com.jclinical.users.domain.ports.out;

import java.time.LocalDateTime;

public interface EmailSenderPort {

    void sendVerificationCode(String email, String fullName, String code, LocalDateTime expiresAt);

    void sendStaffInvitation(String email, String role, String invitationUrl, LocalDateTime expiresAt);

    void sendPasswordReset(String email, String fullName, String resetUrl, LocalDateTime expiresAt);

    void sendShareRecipientVerificationCode(String email, String code, LocalDateTime expiresAt);
}
