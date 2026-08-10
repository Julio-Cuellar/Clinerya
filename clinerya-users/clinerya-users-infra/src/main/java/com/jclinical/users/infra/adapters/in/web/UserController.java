package com.jclinical.users.infra.adapters.in.web;

import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.ClinicStaffInvitation;
import com.jclinical.staff.domain.model.DoctorCredentialStatus;
import com.jclinical.staff.domain.model.DoctorProfile;
import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.ports.out.ClinicStaffInvitationRepositoryPort;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.DoctorProfileRepositoryPort;
import com.jclinical.users.domain.model.Theme;
import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.ports.in.RegisterUserUseCase;
import com.jclinical.users.domain.ports.in.RegisterUserUseCase.RegisterUserCommand;
import com.jclinical.users.domain.ports.in.ResendVerificationCodeUseCase;
import com.jclinical.users.domain.ports.in.VerifyUserEmailUseCase;
import com.jclinical.users.domain.ports.out.PasswordHasherPort;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;
import com.jclinical.users.infra.adapters.in.web.dto.RegisterUserRequest;
import com.jclinical.users.infra.adapters.in.web.dto.ResendVerificationCodeRequest;
import com.jclinical.users.infra.adapters.in.web.dto.UserResponse;
import com.jclinical.users.infra.adapters.in.web.dto.VerifyEmailRequest;
import com.jclinical.users.infra.adapters.out.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyUserEmailUseCase verifyUserEmailUseCase;
    private final ResendVerificationCodeUseCase resendVerificationCodeUseCase;
    private final UserMapper userMapper;

    private final UserRepositoryPort userRepository;
    private final ClinicStaffInvitationRepositoryPort invitationRepository;
    private final ClinicStaffRepositoryPort clinicStaffRepository;
    private final DoctorProfileRepositoryPort doctorProfileRepository;
    private final PasswordHasherPort passwordHasher;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterUserRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(
                request.email(),
                request.password(),
                request.fullName(),
                request.clinicName()
        );

        User registeredUser = registerUserUseCase.registerUser(command);
        UserResponse response = userMapper.toResponse(registeredUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody VerifyEmailRequest request) {
        boolean verified = verifyUserEmailUseCase.verifyEmail(request.token());
        if (verified) {
            return ResponseEntity.ok(Map.of("message", "Correo verificado exitosamente y cuenta activada"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Token de verificación inválido o expirado"));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @RequestBody ResendVerificationCodeRequest request) {
        resendVerificationCodeUseCase.resendVerificationCode(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "Si existe un registro pendiente para ese correo, enviaremos un nuevo código de verificación."));
    }

    @PostMapping("/register-staff")
    public ResponseEntity<Map<String, String>> registerStaff(@RequestBody RegisterStaffInvitationRequest request) {
        if (request.token() == null || request.token().trim().isEmpty()) {
            throw new IllegalArgumentException("El token de invitación es obligatorio.");
        }
        if (request.fullName() == null || request.fullName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio.");
        }
        if (request.password() == null || request.password().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        // 1. Buscar la invitación por token
        ClinicStaffInvitation invitation = invitationRepository.findByTokenAndUsedFalse(request.token().trim())
                .orElseThrow(() -> new IllegalArgumentException("La invitación es inválida, ya fue usada o no existe."));

        if (invitation.isExpired()) {
            throw new IllegalStateException("La invitación ha expirado.");
        }

        // 2. Validar que el correo no esté registrado
        if (userRepository.existsByEmail(invitation.getEmail())) {
            throw new IllegalArgumentException("El correo de la invitación ya se encuentra registrado.");
        }

        // 3. Crear el usuario
        String hashedPassword = passwordHasher.hash(request.password());
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(invitation.getEmail())
                .fullName(request.fullName().trim())
                .passwordHash(hashedPassword)
                .emailVerified(true)
                .active(true)
                .themePreference(Theme.LIGHT)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // 4. Crear el miembro de personal (ClinicStaff)
        ClinicStaff staff = ClinicStaff.builder()
                .id(UUID.randomUUID())
                .clinicId(invitation.getClinicId())
                .userId(userId)
                .role(invitation.getRole())
                .active(true)
                .hireDate(java.time.LocalDate.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        clinicStaffRepository.save(staff);

        // 5. Si el rol es DOCTOR, crear perfil de doctor
        if (invitation.getRole() == StaffRole.DOCTOR) {
            DoctorProfile doctorProfile = DoctorProfile.builder()
                    .id(UUID.randomUUID())
                    .clinicId(invitation.getClinicId())
                    .clinicStaffId(staff.getId())
                    .credentialStatus(DoctorCredentialStatus.EN_TRAMITE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            doctorProfileRepository.save(doctorProfile);
        }

        // 6. Marcar la invitación como usada
        invitation.setUsed(true);
        invitationRepository.save(invitation);

        return ResponseEntity.ok(Map.of("message", "Personal registrado exitosamente en la clínica"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody ChangePasswordRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Usuario no autenticado"));
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (request.currentPassword() == null || request.currentPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña actual es obligatoria.");
        }
        if (request.newPassword() == null || request.newPassword().trim().isEmpty() || request.newPassword().length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres.");
        }

        if (!passwordHasher.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }

        String hashed = passwordHasher.hash(request.newPassword());
        user.setPasswordHash(hashed);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Contraseña cambiada exitosamente"));
    }

    @PutMapping("/theme")
    public ResponseEntity<Map<String, String>> updateTheme(@RequestBody UpdateThemeRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Usuario no autenticado"));
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        try {
            Theme newTheme = Theme.valueOf(request.theme().toUpperCase());
            user.changeTheme(newTheme);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Preferencia de tema actualizada"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Tema inválido: debe ser LIGHT o DARK");
        }
    }

    public record ChangePasswordRequest(
            String currentPassword,
            String newPassword
    ) {}

    public record UpdateThemeRequest(
            String theme
    ) {}

    public record RegisterStaffInvitationRequest(
            String token,
            String fullName,
            String password
    ) {}
}
