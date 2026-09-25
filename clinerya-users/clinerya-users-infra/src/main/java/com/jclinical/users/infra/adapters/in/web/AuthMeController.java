package com.jclinical.users.infra.adapters.in.web;

import com.jclinical.clinics.domain.model.Clinic;
import com.jclinical.clinics.domain.ports.in.ManageClinicUseCase;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;
import com.jclinical.users.infra.adapters.in.web.dto.UserMeResponse;
import com.jclinical.users.infra.adapters.in.web.dto.UserMeResponse.ClinicDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthMeController {

    private final UserRepositoryPort userRepository;
    private final ManageClinicUseCase manageClinicUseCase;
    private final ManageClinicStaffUseCase clinicStaffUseCase;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMe() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Clinic> clinics = manageClinicUseCase.getClinicsByOwner(user.getId());
        List<ClinicDto> clinicDtos = clinics.stream()
                .map(c -> {
                    var staff = clinicStaffUseCase.getActiveStaffByUserAndClinic(user.getId(), c.getId());
                    String role = staff.map(value -> value.role().name()).orElse("DOCTOR");
                    List<String> permissions = staff
                            .map(value -> clinicStaffUseCase.getPermissions(c.getId(), value.staffId()).permissions().stream()
                                    .filter(ManageClinicStaffUseCase.PermissionItem::enabled)
                                    .map(item -> item.permission().name())
                                    .toList())
                            .orElse(List.of());
                    boolean attendsPatients = staff.map(ManageClinicStaffUseCase.StaffSummary::practitioner).orElse(false);
                    return new ClinicDto(c.getId(), c.getName(), role, permissions, attendsPatients);
                })
                .toList();

        String theme = user.getThemePreference() != null ? user.getThemePreference().name() : "LIGHT";

        UserMeResponse response = new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                theme,
                user.isActive(),
                clinicDtos
        );

        return ResponseEntity.ok(response);
    }
}
