package com.jclinical.users.domain.service;

import com.jclinical.users.domain.model.UserPreRegistration;
import com.jclinical.users.domain.ports.in.ResendVerificationCodeUseCase;
import com.jclinical.users.domain.ports.out.EmailSenderPort;
import com.jclinical.users.domain.ports.out.UserPreRegistrationRepositoryPort;

import java.time.LocalDateTime;

public class ResendVerificationCodeService implements ResendVerificationCodeUseCase {

    private static final int CODE_EXPIRATION_MINUTES = 15;

    private final UserPreRegistrationRepositoryPort preRegistrationRepository;
    private final EmailSenderPort emailSender;

    public ResendVerificationCodeService(
            UserPreRegistrationRepositoryPort preRegistrationRepository,
            EmailSenderPort emailSender) {
        this.preRegistrationRepository = preRegistrationRepository;
        this.emailSender = emailSender;
    }

    @Override
    public boolean resendVerificationCode(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        return preRegistrationRepository.findByEmail(email.trim().toLowerCase())
                .map(this::refreshAndSend)
                .orElse(false);
    }

    private boolean refreshAndSend(UserPreRegistration preRegistration) {
        String code = VerificationCodeGenerator.generate();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES);
        preRegistration.setVerificationToken(code);
        preRegistration.setVerificationTokenExpiresAt(expiresAt);
        preRegistrationRepository.save(preRegistration);
        emailSender.sendVerificationCode(
                preRegistration.getEmail(), preRegistration.getFullName(), code, expiresAt);
        return true;
    }
}
