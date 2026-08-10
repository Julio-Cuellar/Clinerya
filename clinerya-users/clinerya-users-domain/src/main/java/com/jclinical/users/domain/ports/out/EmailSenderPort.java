package com.jclinical.users.domain.ports.out;

import java.time.LocalDateTime;

public interface EmailSenderPort {

    void sendVerificationCode(String email, String fullName, String code, LocalDateTime expiresAt);
}
