package com.jclinical.staff.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.ClinicStaffInvitation;
import com.jclinical.staff.domain.model.DoctorCredentialStatus;
import com.jclinical.staff.domain.model.DoctorProfile;
import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.staff.domain.model.StaffPermissionOverride;
import com.jclinical.staff.domain.model.StaffPermissionOverrideState;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.ClinicStaffInvitationRepositoryPort;
import com.jclinical.staff.domain.ports.out.DoctorProfileRepositoryPort;
import com.jclinical.staff.domain.ports.out.UserDirectoryPort;
import com.jclinical.staff.domain.ports.out.StaffPermissionOverrideRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffInvitationNotifierPort;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClinicStaffService implements ManageClinicStaffUseCase {
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    private final ClinicStaffRepositoryPort clinicStaffRepository;
    private final DoctorProfileRepositoryPort doctorProfileRepository;
    private final UserDirectoryPort userDirectory;
    private final ClinicStaffInvitationRepositoryPort invitationRepository;
    private final StaffPermissionOverrideRepositoryPort permissionOverrideRepository;
    private final StaffInvitationNotifierPort invitationNotifier;

    public ClinicStaffService(ClinicStaffRepositoryPort clinicStaffRepository,
                               DoctorProfileRepositoryPort doctorProfileRepository,
                               UserDirectoryPort userDirectory,
                               ClinicStaffInvitationRepositoryPort invitationRepository,
                               StaffPermissionOverrideRepositoryPort permissionOverrideRepository,
                               StaffInvitationNotifierPort invitationNotifier) {
        this.clinicStaffRepository = clinicStaffRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userDirectory = userDirectory;
        this.invitationRepository = invitationRepository;
        this.permissionOverrideRepository = permissionOverrideRepository;
        this.invitationNotifier = invitationNotifier;
    }

    @Override
    public List<StaffSummary> listStaffByClinic(UUID clinicId, StaffRole role) {
        return clinicStaffRepository.findByClinicId(clinicId).stream()
                .filter(ClinicStaff::isActive)
                .filter(staff -> role == null || staff.getRole() == role)
                .map(this::toSummary)
                .toList();
    }

    @Override
    public Optional<StaffSummary> getActiveStaffById(UUID staffId, UUID clinicId) {
        return clinicStaffRepository.findById(staffId)
                .filter(staff -> staff.getClinicId().equals(clinicId))
                .filter(ClinicStaff::isActive)
                .map(this::toSummary);
    }

    @Override
    public Optional<StaffSummary> getActiveStaffByUserAndClinic(UUID userId, UUID clinicId) {
        return clinicStaffRepository.findByClinicIdAndUserId(clinicId, userId)
                .filter(ClinicStaff::isActive)
                .map(this::toSummary);
    }

    @Override
    public StaffSummary addStaff(UUID clinicId, UUID actingUserId, String email, StaffRole role) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_STAFF);
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio.");
        }
        UserDirectoryPort.UserSummary userSummary = userDirectory.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("No existe ningún usuario registrado con el correo: " + email));

        Optional<ClinicStaff> existing = clinicStaffRepository.findByClinicIdAndUserId(clinicId, userSummary.id());
        if (existing.isPresent()) {
            ClinicStaff existingStaff = existing.get();
            if (existingStaff.isActive()) {
                throw new IllegalStateException("El usuario ya es un miembro activo del personal de esta clínica.");
            } else {
                existingStaff.setActive(true);
                existingStaff.setRole(role);
                existingStaff.setUpdatedAt(LocalDateTime.now());
                clinicStaffRepository.save(existingStaff);
                return toSummary(existingStaff);
            }
        }

        ClinicStaff staff = ClinicStaff.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .userId(userSummary.id())
                .role(role)
                .active(true)
                .hireDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        clinicStaffRepository.save(staff);

        if (role == StaffRole.DOCTOR) {
            DoctorProfile doctorProfile = DoctorProfile.builder()
                    .id(UUID.randomUUID())
                    .clinicId(clinicId)
                    .clinicStaffId(staff.getId())
                    .credentialStatus(DoctorCredentialStatus.EN_TRAMITE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            doctorProfileRepository.save(doctorProfile);
        }

        return toSummary(staff);
    }

    @Override
    public StaffInvitationSummary inviteStaff(UUID clinicId, UUID actingUserId, String email, StaffRole role) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_STAFF);
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio.");
        }
        String normalizedEmail = email.trim().toLowerCase();

        // 1. Validar que el correo no esté registrado en la base de datos de usuarios
        if (userDirectory.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("El correo ya está registrado en Clinerya.");
        }

        // 2. Validar que no haya una invitación activa ya enviada para este correo en esta clínica
        Optional<ClinicStaffInvitation> existing = invitationRepository.findByClinicIdAndEmailAndUsedFalse(clinicId, normalizedEmail);
        if (existing.isPresent()) {
            ClinicStaffInvitation activeInv = existing.get();
            if (!activeInv.isExpired()) {
                invitationNotifier.sendInvitation(normalizedEmail, activeInv.getRole(), activeInv.getToken(), activeInv.getExpiresAt());
                return toInvitationSummary(activeInv);
            }
        }

        String token = generateInvitationToken();

        ClinicStaffInvitation invitation = ClinicStaffInvitation.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .email(normalizedEmail)
                .role(role)
                .token(token)
                .used(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1)) // 24 horas de vigencia
                .build();

        invitationRepository.save(invitation);

        invitationNotifier.sendInvitation(normalizedEmail, role, token, invitation.getExpiresAt());

        return toInvitationSummary(invitation);
    }

    @Override
    public List<StaffInvitationSummary> listInvitations(UUID clinicId) {
        return invitationRepository.findByClinicIdAndUsedFalse(clinicId).stream()
                .filter(inv -> !inv.isExpired())
                .map(this::toInvitationSummary)
                .toList();
    }

    @Override
    public StaffSummary updateStaff(UUID clinicId, UUID actingUserId, UUID staffId, StaffRole role) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_STAFF);
        ClinicStaff staff = clinicStaffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Miembro del personal no encontrado"));
        if (!staff.getClinicId().equals(clinicId)) {
            throw new IllegalArgumentException("El miembro del personal no pertenece a esta clínica");
        }
        if (staff.getRole() == StaffRole.ADMIN && role != StaffRole.ADMIN) {
            throw new IllegalStateException("El superadministrador no puede cambiar de rol.");
        }
        staff.setRole(role);
        staff.setUpdatedAt(LocalDateTime.now());
        clinicStaffRepository.save(staff);

        if (role == StaffRole.DOCTOR) {
            if (doctorProfileRepository.findByClinicStaffId(staffId).isEmpty()) {
                DoctorProfile doctorProfile = DoctorProfile.builder()
                        .id(UUID.randomUUID())
                        .clinicId(clinicId)
                        .clinicStaffId(staff.getId())
                        .credentialStatus(DoctorCredentialStatus.EN_TRAMITE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                doctorProfileRepository.save(doctorProfile);
            }
        }

        return toSummary(staff);
    }

    @Override
    public PermissionSummary getPermissions(UUID clinicId, UUID staffId) {
        ClinicStaff staff = findActiveStaff(clinicId, staffId);
        boolean superAdmin = staff.getRole() == StaffRole.ADMIN;
        Map<StaffPermission, StaffPermissionOverrideState> overrides = permissionOverrideRepository
                .findByClinicIdAndStaffId(clinicId, staffId).stream()
                .collect(Collectors.toMap(StaffPermissionOverride::getPermission, StaffPermissionOverride::getState));
        Set<StaffPermission> defaults = defaultPermissions(staff.getRole());

        List<ManageClinicStaffUseCase.PermissionItem> items = List.of(StaffPermission.values()).stream()
                .map(permission -> {
                    StaffPermissionOverrideState state = superAdmin
                            ? StaffPermissionOverrideState.INHERIT
                            : overrides.getOrDefault(permission, StaffPermissionOverrideState.INHERIT);
                    boolean enabled = superAdmin || state == StaffPermissionOverrideState.GRANTED
                            || (state == StaffPermissionOverrideState.INHERIT && defaults.contains(permission));
                    return new ManageClinicStaffUseCase.PermissionItem(permission, state, enabled);
                })
                .toList();

        return new PermissionSummary(staffId, staff.getRole(), items);
    }

    @Override
    public PermissionSummary updatePermissions(UUID clinicId, UUID actingUserId, UUID staffId, List<PermissionChange> changes) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_STAFF_PERMISSIONS);
        ClinicStaff staff = findActiveStaff(clinicId, staffId);
        if (staff.getRole() == StaffRole.ADMIN) {
            throw new IllegalStateException("Los permisos del superadministrador no se pueden modificar.");
        }
        if (changes != null) {
            for (PermissionChange change : changes) {
                if (change == null || change.permission() == null || change.state() == null) {
                    continue;
                }
                Optional<StaffPermissionOverride> existing = permissionOverrideRepository
                        .findByClinicIdAndStaffIdAndPermission(clinicId, staffId, change.permission());
                if (change.state() == StaffPermissionOverrideState.INHERIT) {
                    existing.ifPresent(permissionOverrideRepository::delete);
                    continue;
                }

                StaffPermissionOverride override = existing.orElseGet(() -> StaffPermissionOverride.builder()
                        .id(UUID.randomUUID())
                        .clinicId(clinicId)
                        .staffId(staffId)
                        .permission(change.permission())
                        .createdAt(LocalDateTime.now())
                        .build());
                override.setState(change.state());
                override.setUpdatedAt(LocalDateTime.now());
                permissionOverrideRepository.save(override);
            }
        }
        return getPermissions(clinicId, staffId);
    }

    @Override
    public void removeStaff(UUID clinicId, UUID actingUserId, UUID staffId) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_STAFF);
        ClinicStaff staff = clinicStaffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Miembro del personal no encontrado"));
        if (!staff.getClinicId().equals(clinicId)) {
            throw new IllegalArgumentException("El miembro del personal no pertenece a esta clínica");
        }
        staff.setActive(false);
        staff.setUpdatedAt(LocalDateTime.now());
        clinicStaffRepository.save(staff);
    }

    private void requirePermission(UUID clinicId, UUID actingUserId, StaffPermission permission) {
        ClinicStaff actingStaff = clinicStaffRepository.findByClinicIdAndUserId(clinicId, actingUserId)
                .filter(ClinicStaff::isActive)
                .orElseThrow(() -> new ClinicAccessDeniedException("No perteneces al personal de esta clínica."));
        boolean granted = getPermissions(clinicId, actingStaff.getId()).permissions().stream()
                .anyMatch(item -> item.permission() == permission && item.enabled());
        if (!granted) {
            throw new ClinicAccessDeniedException("No tienes permiso para gestionar al personal de esta clínica.");
        }
    }

    private ClinicStaff findActiveStaff(UUID clinicId, UUID staffId) {
        return clinicStaffRepository.findById(staffId)
                .filter(staff -> staff.getClinicId().equals(clinicId))
                .filter(ClinicStaff::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Miembro del personal no encontrado o inactivo."));
    }

    private Set<StaffPermission> defaultPermissions(StaffRole role) {
        if (role == StaffRole.ADMIN || role == StaffRole.CLINIC_ADMIN) {
            return EnumSet.allOf(StaffPermission.class);
        }
        return switch (role) {
            case DOCTOR -> EnumSet.of(
                    StaffPermission.VIEW_DASHBOARD,
                    StaffPermission.VIEW_DASHBOARD_METRICS,
                    StaffPermission.MANAGE_AGENDA,
                    StaffPermission.VIEW_AGENDA,
                    StaffPermission.CREATE_APPOINTMENTS,
                    StaffPermission.EDIT_APPOINTMENTS,
                    StaffPermission.MANAGE_PATIENTS,
                    StaffPermission.VIEW_PATIENTS,
                    StaffPermission.VIEW_MEDICAL_RECORDS,
                    StaffPermission.EDIT_MEDICAL_RECORDS,
                    StaffPermission.CREATE_CLINICAL_NOTES,
                    StaffPermission.EDIT_CLINICAL_NOTES,
                    StaffPermission.VIEW_PATIENT_CARE,
                    StaffPermission.MANAGE_PATIENT_CARE,
                    StaffPermission.CREATE_VISITS,
                    StaffPermission.EDIT_VISITS,
                    StaffPermission.MANAGE_PRESCRIPTIONS,
                    StaffPermission.MANAGE_TREATMENTS,
                    StaffPermission.VIEW_TREATMENTS,
                    StaffPermission.MANAGE_PROCEDURES,
                    StaffPermission.VIEW_ROOMS);
            case RECEPTIONIST -> EnumSet.of(
                    StaffPermission.VIEW_DASHBOARD,
                    StaffPermission.VIEW_OPERATIONAL_ALERTS,
                    StaffPermission.MANAGE_AGENDA,
                    StaffPermission.VIEW_AGENDA,
                    StaffPermission.CREATE_APPOINTMENTS,
                    StaffPermission.EDIT_APPOINTMENTS,
                    StaffPermission.CANCEL_APPOINTMENTS,
                    StaffPermission.MANAGE_WAITING_LIST,
                    StaffPermission.MANAGE_PATIENTS,
                    StaffPermission.VIEW_PATIENTS,
                    StaffPermission.CREATE_PATIENTS,
                    StaffPermission.EDIT_PATIENTS,
                    StaffPermission.MANAGE_CASH,
                    StaffPermission.VIEW_CASH,
                    StaffPermission.CREATE_CHARGES,
                    StaffPermission.VIEW_TREATMENTS,
                    StaffPermission.MANAGE_QUOTATIONS,
                    StaffPermission.VIEW_ROOMS);
            case ASSISTANT -> EnumSet.of(
                    StaffPermission.VIEW_DASHBOARD,
                    StaffPermission.MANAGE_AGENDA,
                    StaffPermission.VIEW_AGENDA,
                    StaffPermission.CREATE_APPOINTMENTS,
                    StaffPermission.EDIT_APPOINTMENTS,
                    StaffPermission.MANAGE_PATIENTS,
                    StaffPermission.VIEW_PATIENTS,
                    StaffPermission.EDIT_PATIENTS,
                    StaffPermission.VIEW_MEDICAL_RECORDS,
                    StaffPermission.VIEW_RECORD_DOCUMENTS,
                    StaffPermission.VIEW_PATIENT_CARE,
                    StaffPermission.MANAGE_PATIENT_CARE,
                    StaffPermission.MANAGE_INVENTORY,
                    StaffPermission.VIEW_INVENTORY,
                    StaffPermission.MANAGE_INVENTORY_MOVEMENTS,
                    StaffPermission.VIEW_TREATMENTS,
                    StaffPermission.VIEW_ROOMS);
            case ACCOUNTANT -> EnumSet.of(
                    StaffPermission.VIEW_DASHBOARD,
                    StaffPermission.VIEW_DASHBOARD_METRICS,
                    StaffPermission.MANAGE_CASH,
                    StaffPermission.VIEW_CASH,
                    StaffPermission.MANAGE_CASH_CUTS,
                    StaffPermission.MANAGE_INVENTORY,
                    StaffPermission.VIEW_INVENTORY,
                    StaffPermission.VIEW_ACCOUNTING,
                    StaffPermission.MANAGE_ACCOUNTING,
                    StaffPermission.VIEW_JOURNAL_ENTRIES,
                    StaffPermission.CREATE_JOURNAL_ENTRIES,
                    StaffPermission.VIEW_FINANCIAL_REPORTS,
                    StaffPermission.MANAGE_BANK_ACCOUNTS,
                    StaffPermission.VIEW_PAYROLL);
            case CLEANING -> EnumSet.of(
                    StaffPermission.VIEW_DASHBOARD,
                    StaffPermission.VIEW_AGENDA,
                    StaffPermission.VIEW_ROOMS);
            default -> EnumSet.noneOf(StaffPermission.class);
        };
    }

    private StaffInvitationSummary toInvitationSummary(ClinicStaffInvitation inv) {
        return new StaffInvitationSummary(
                inv.getId(),
                inv.getClinicId(),
                inv.getEmail(),
                inv.getRole(),
                inv.getToken(),
                inv.isUsed(),
                inv.getExpiresAt()
        );
    }

    private String generateInvitationToken() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(TOKEN_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private StaffSummary toSummary(ClinicStaff staff) {
        String fullName = userDirectory.findUser(staff.getUserId())
                .map(UserDirectoryPort.UserSummary::fullName)
                .orElse("Usuario desconocido");
        return new StaffSummary(staff.getId(), staff.getClinicId(), staff.getUserId(), staff.getRole(), fullName);
    }
}
