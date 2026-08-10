package com.jclinical.users.domain.service;

import com.jclinical.users.domain.model.Theme;
import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.model.UserPreRegistration;
import com.jclinical.users.domain.ports.in.RegisterUserUseCase;
import com.jclinical.users.domain.ports.out.EmailSenderPort;
import com.jclinical.users.domain.ports.out.PasswordHasherPort;
import com.jclinical.users.domain.ports.out.UserPreRegistrationRepositoryPort;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;

import java.time.LocalDateTime;
import java.util.UUID;

public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepositoryPort userRepository;
    private final UserPreRegistrationRepositoryPort preRegistrationRepository;
    private final PasswordHasherPort passwordHasher;
    private final EmailSenderPort emailSender;

    public RegisterUserService(UserRepositoryPort userRepository,
                               UserPreRegistrationRepositoryPort preRegistrationRepository,
                               PasswordHasherPort passwordHasher,
                               EmailSenderPort emailSender) {
        this.userRepository = userRepository;
        this.preRegistrationRepository = preRegistrationRepository;
        this.passwordHasher = passwordHasher;
        this.emailSender = emailSender;
    }

    @Override
    public User registerUser(RegisterUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado");
        }

        // Eliminar pre-registro anterior si existe para el mismo correo (reintento)
        preRegistrationRepository.findByEmail(command.email()).ifPresent(preReg -> {
            preRegistrationRepository.deleteById(preReg.getId());
        });

        String hashedPassword = passwordHasher.hash(command.password());
        String token = VerificationCodeGenerator.generate();
        UUID tempUserId = UUID.randomUUID();

        UserPreRegistration preReg = UserPreRegistration.builder()
                .id(tempUserId)
                .email(command.email())
                .fullName(command.fullName())
                .passwordHash(hashedPassword)
                .clinicName(command.clinicName())
                .verificationToken(token)
                .verificationTokenExpiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now())
                .build();

        preRegistrationRepository.save(preReg);
        emailSender.sendVerificationCode(
                preReg.getEmail(), preReg.getFullName(), token, preReg.getVerificationTokenExpiresAt());

        // Retornar objeto User transitorio para la respuesta del controlador
        return User.builder()
                .id(tempUserId)
                .email(command.email())
                .fullName(command.fullName())
                .active(false)
                .emailVerified(false)
                .themePreference(Theme.LIGHT)
                .failedLoginAttempts(0)
                .createdAt(preReg.getCreatedAt())
                .updatedAt(preReg.getCreatedAt())
                .build();
    }

}
