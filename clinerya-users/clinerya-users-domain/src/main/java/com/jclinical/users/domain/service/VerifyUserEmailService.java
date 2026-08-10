package com.jclinical.users.domain.service;

import com.jclinical.users.domain.model.Theme;
import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.model.UserPreRegistration;
import com.jclinical.users.domain.model.events.UserEvents.UserEmailVerifiedEvent;
import com.jclinical.users.domain.ports.in.VerifyUserEmailUseCase;
import com.jclinical.users.domain.ports.out.EventPublisherPort;
import com.jclinical.users.domain.ports.out.UserPreRegistrationRepositoryPort;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;

import java.time.LocalDateTime;
import java.util.Optional;

public class VerifyUserEmailService implements VerifyUserEmailUseCase {

    private final UserRepositoryPort userRepository;
    private final UserPreRegistrationRepositoryPort preRegistrationRepository;
    private final EventPublisherPort eventPublisher;

    public VerifyUserEmailService(UserRepositoryPort userRepository,
                                  UserPreRegistrationRepositoryPort preRegistrationRepository,
                                  EventPublisherPort eventPublisher) {
        this.userRepository = userRepository;
        this.preRegistrationRepository = preRegistrationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public boolean verifyEmail(String tokenValue) {
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            throw new IllegalArgumentException("El token de verificación no puede ser vacío");
        }

        Optional<UserPreRegistration> preRegOpt = preRegistrationRepository.findByVerificationToken(tokenValue.trim().toUpperCase());
        if (preRegOpt.isEmpty()) {
            return false;
        }

        UserPreRegistration preReg = preRegOpt.get();
        if (preReg.isExpired()) {
            throw new IllegalStateException("El token de verificación ha expirado (límite de 15 minutos)");
        }

        // Promover el pre-registro a un usuario definitivo activo
        User user = User.builder()
                .id(preReg.getId())
                .email(preReg.getEmail())
                .fullName(preReg.getFullName())
                .passwordHash(preReg.getPasswordHash())
                .emailVerified(true)
                .active(true)
                .themePreference(Theme.LIGHT)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        // Eliminar del pre-registro
        preRegistrationRepository.deleteById(preReg.getId());

        // Publicar evento de dominio para activar la clínica y sus respectivos módulos de forma asíncrona
        eventPublisher.publish(new UserEmailVerifiedEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                preReg.getFullName(),
                preReg.getClinicName()
        ));

        return true;
    }
}
