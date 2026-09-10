package com.jclinical.staff.infra.adapters.in.web;

import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.staff.domain.model.StaffPermissionOverrideState;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase.StaffSummary;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase.StaffInvitationSummary;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/staff")
@RequiredArgsConstructor
public class ClinicStaffController {

    private final ManageClinicStaffUseCase manageClinicStaffUseCase;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public ResponseEntity<List<StaffSummary>> getStaff(
            @PathVariable UUID clinicId,
            @RequestParam(required = false) String role) {
        StaffRole staffRole = null;
        if (role != null && !role.trim().isEmpty()) {
            try {
                staffRole = StaffRole.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Rol de personal no válido: " + role);
            }
        }
        List<StaffSummary> staffList = manageClinicStaffUseCase.listStaffByClinic(clinicId, staffRole);
        return ResponseEntity.ok(staffList);
    }

    @PostMapping
    public ResponseEntity<StaffSummary> addStaff(
            @PathVariable UUID clinicId,
            @RequestBody AddStaffRequest request) {
        if (request.role() == null || request.role().trim().isEmpty()) {
            throw new IllegalArgumentException("El rol de personal es obligatorio.");
        }
        StaffRole staffRole;
        try {
            staffRole = StaffRole.valueOf(request.role().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol de personal no válido: " + request.role());
        }
        StaffSummary summary = manageClinicStaffUseCase.addStaff(
                clinicId, currentUserResolver.getCurrentUserId(), request.email(), staffRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @PostMapping("/invitations")
    public ResponseEntity<StaffInvitationSummary> inviteStaff(
            @PathVariable UUID clinicId,
            @RequestBody AddStaffRequest request) {
        if (request.role() == null || request.role().trim().isEmpty()) {
            throw new IllegalArgumentException("El rol de personal es obligatorio.");
        }
        StaffRole staffRole;
        try {
            staffRole = StaffRole.valueOf(request.role().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol de personal no válido: " + request.role());
        }
        StaffInvitationSummary summary = manageClinicStaffUseCase.inviteStaff(
                clinicId, currentUserResolver.getCurrentUserId(), request.email(), staffRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @GetMapping("/invitations")
    public ResponseEntity<List<StaffInvitationSummary>> getInvitations(@PathVariable UUID clinicId) {
        List<StaffInvitationSummary> invitations = manageClinicStaffUseCase.listInvitations(clinicId);
        return ResponseEntity.ok(invitations);
    }

    @PutMapping("/{staffId}")
    public ResponseEntity<StaffSummary> updateStaff(
            @PathVariable UUID clinicId,
            @PathVariable UUID staffId,
            @RequestBody UpdateStaffRequest request) {
        if (request.role() == null || request.role().trim().isEmpty()) {
            throw new IllegalArgumentException("El rol de personal es obligatorio.");
        }
        StaffRole staffRole;
        try {
            staffRole = StaffRole.valueOf(request.role().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol de personal no válido: " + request.role());
        }
        StaffSummary summary = manageClinicStaffUseCase.updateStaff(
                clinicId, currentUserResolver.getCurrentUserId(), staffId, staffRole);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{staffId}/permissions")
    public ResponseEntity<ManageClinicStaffUseCase.PermissionSummary> getPermissions(
            @PathVariable UUID clinicId,
            @PathVariable UUID staffId) {
        return ResponseEntity.ok(manageClinicStaffUseCase.getPermissions(clinicId, staffId));
    }

    @PutMapping("/{staffId}/permissions")
    public ResponseEntity<ManageClinicStaffUseCase.PermissionSummary> updatePermissions(
            @PathVariable UUID clinicId,
            @PathVariable UUID staffId,
            @RequestBody UpdatePermissionsRequest request) {
        List<ManageClinicStaffUseCase.PermissionChange> changes = request == null || request.permissions() == null
                ? List.of()
                : request.permissions().stream()
                .map(this::toPermissionChange)
                .toList();
        return ResponseEntity.ok(manageClinicStaffUseCase.updatePermissions(
                clinicId, currentUserResolver.getCurrentUserId(), staffId, changes));
    }

    @DeleteMapping("/{staffId}")
    public ResponseEntity<Void> removeStaff(
            @PathVariable UUID clinicId,
            @PathVariable UUID staffId) {
        manageClinicStaffUseCase.removeStaff(clinicId, currentUserResolver.getCurrentUserId(), staffId);
        return ResponseEntity.noContent().build();
    }

    public record AddStaffRequest(
            String email,
            String role
    ) {}

    public record UpdateStaffRequest(
            String role
    ) {}

    public record UpdatePermissionsRequest(
            List<PermissionChangeRequest> permissions
    ) {}

    public record PermissionChangeRequest(
            String permission,
            String state
    ) {}

    private ManageClinicStaffUseCase.PermissionChange toPermissionChange(PermissionChangeRequest request) {
        if (request == null || request.permission() == null || request.state() == null) {
            throw new IllegalArgumentException("El permiso y su estado son obligatorios.");
        }
        try {
            return new ManageClinicStaffUseCase.PermissionChange(
                    StaffPermission.valueOf(request.permission().trim().toUpperCase()),
                    StaffPermissionOverrideState.valueOf(request.state().trim().toUpperCase()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Permiso o estado de permiso no valido.");
        }
    }
}
