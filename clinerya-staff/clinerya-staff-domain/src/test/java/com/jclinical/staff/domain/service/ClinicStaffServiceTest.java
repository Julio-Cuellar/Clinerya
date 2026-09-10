package com.jclinical.staff.domain.service;

import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.ClinicStaffInvitation;
import com.jclinical.staff.domain.model.DoctorProfile;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.staff.domain.model.StaffPermissionOverride;
import com.jclinical.staff.domain.model.StaffPermissionOverrideState;
import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase.PermissionChange;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase.PermissionSummary;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase.StaffInvitationSummary;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase.StaffSummary;
import com.jclinical.staff.domain.ports.out.ClinicStaffInvitationRepositoryPort;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.DoctorProfileRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPermissionOverrideRepositoryPort;
import com.jclinical.staff.domain.ports.out.UserDirectoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClinicStaffServiceTest {

    private final InMemoryClinicStaffRepository staffRepository = new InMemoryClinicStaffRepository();
    private final InMemoryDoctorProfileRepository doctorProfileRepository = new InMemoryDoctorProfileRepository();
    private final InMemoryUserDirectory userDirectory = new InMemoryUserDirectory();
    private final InMemoryInvitationRepository invitationRepository = new InMemoryInvitationRepository();
    private final InMemoryPermissionOverrideRepository permissionRepository = new InMemoryPermissionOverrideRepository();
    private ClinicStaffService service;

    @BeforeEach
    void setUp() {
        service = new ClinicStaffService(
                staffRepository,
                doctorProfileRepository,
                userDirectory,
                invitationRepository,
                permissionRepository);
    }

    @Test
    void addsDoctorCreatesProfileAndListsActiveStaff() {
        UUID clinicId = UUID.randomUUID();
        UUID userId = userDirectory.addUser("doctor@clinerya.com", "Dra. Ana Lopez");

        StaffSummary summary = service.addStaff(clinicId, " Doctor@Clinerya.com ", StaffRole.DOCTOR);

        assertEquals(clinicId, summary.clinicId());
        assertEquals(userId, summary.userId());
        assertEquals(StaffRole.DOCTOR, summary.role());
        assertEquals("Dra. Ana Lopez", summary.fullName());
        assertEquals(1, doctorProfileRepository.profiles.size());
        assertEquals(1, service.listStaffByClinic(clinicId, StaffRole.DOCTOR).size());
        assertTrue(service.getActiveStaffByUserAndClinic(userId, clinicId).isPresent());
        assertTrue(service.getActiveStaffById(summary.staffId(), clinicId).isPresent());
    }

    @Test
    void rejectsDuplicateActiveStaffAndReactivatesInactiveStaff() {
        UUID clinicId = UUID.randomUUID();
        UUID userId = userDirectory.addUser("recepcion@clinerya.com", "Recepcion");
        StaffSummary first = service.addStaff(clinicId, "recepcion@clinerya.com", StaffRole.RECEPTIONIST);

        assertThrows(IllegalStateException.class,
                () -> service.addStaff(clinicId, "recepcion@clinerya.com", StaffRole.ASSISTANT));

        service.removeStaff(clinicId, first.staffId());
        StaffSummary reactivated = service.addStaff(clinicId, "recepcion@clinerya.com", StaffRole.ASSISTANT);

        assertEquals(first.staffId(), reactivated.staffId());
        assertEquals(userId, reactivated.userId());
        assertEquals(StaffRole.ASSISTANT, reactivated.role());
        assertTrue(staffRepository.findById(first.staffId()).orElseThrow().isActive());
    }

    @Test
    void managesInvitationsAndFiltersExpiredOnes() {
        UUID clinicId = UUID.randomUUID();
        userDirectory.addUser("registrado@clinerya.com", "Registrado");

        assertThrows(IllegalArgumentException.class,
                () -> service.inviteStaff(clinicId, "registrado@clinerya.com", StaffRole.ACCOUNTANT));

        StaffInvitationSummary invitation = service.inviteStaff(clinicId, " Nuevo@Clinerya.com ", StaffRole.ACCOUNTANT);
        StaffInvitationSummary reused = service.inviteStaff(clinicId, "nuevo@clinerya.com", StaffRole.ACCOUNTANT);

        assertEquals(invitation.invitationId(), reused.invitationId());
        assertEquals("nuevo@clinerya.com", invitation.email());
        assertNotNull(invitation.token());

        invitationRepository.save(ClinicStaffInvitation.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .email("expirada@clinerya.com")
                .role(StaffRole.ASSISTANT)
                .token("expired")
                .used(false)
                .createdAt(LocalDateTime.now().minusDays(2))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build());

        assertEquals(1, service.listInvitations(clinicId).size());
    }

    @Test
    void updatesRolesCreatesDoctorProfileAndProtectsAdminRole() {
        UUID clinicId = UUID.randomUUID();
        UUID assistantId = userDirectory.addUser("asistente@clinerya.com", "Asistente");
        StaffSummary assistant = service.addStaff(clinicId, "asistente@clinerya.com", StaffRole.ASSISTANT);

        StaffSummary updated = service.updateStaff(clinicId, assistant.staffId(), StaffRole.DOCTOR);

        assertEquals(assistantId, updated.userId());
        assertEquals(StaffRole.DOCTOR, updated.role());
        assertEquals(1, doctorProfileRepository.profiles.size());

        ClinicStaff admin = staff(clinicId, userDirectory.addUser("admin@clinerya.com", "Admin"), StaffRole.ADMIN);
        staffRepository.save(admin);

        assertThrows(IllegalStateException.class,
                () -> service.updateStaff(clinicId, admin.getId(), StaffRole.RECEPTIONIST));
    }

    @Test
    void calculatesAndUpdatesPermissionOverrides() {
        UUID clinicId = UUID.randomUUID();
        StaffSummary receptionist = service.addStaff(
                clinicId,
                emailFor(userDirectory.addUser("permisos@clinerya.com", "Permisos")),
                StaffRole.RECEPTIONIST);

        PermissionSummary defaults = service.getPermissions(clinicId, receptionist.staffId());

        assertTrue(enabled(defaults, StaffPermission.VIEW_AGENDA));
        assertFalse(enabled(defaults, StaffPermission.MANAGE_ACCOUNTING));

        List<PermissionChange> changes = new ArrayList<>();
        changes.add(new PermissionChange(StaffPermission.MANAGE_ACCOUNTING, StaffPermissionOverrideState.GRANTED));
        changes.add(new PermissionChange(StaffPermission.VIEW_AGENDA, StaffPermissionOverrideState.REVOKED));
        changes.add(null);
        PermissionSummary updated = service.updatePermissions(clinicId, receptionist.staffId(), changes);

        assertTrue(enabled(updated, StaffPermission.MANAGE_ACCOUNTING));
        assertFalse(enabled(updated, StaffPermission.VIEW_AGENDA));
        assertEquals(2, permissionRepository.overrides.size());

        PermissionSummary inheritedAgain = service.updatePermissions(clinicId, receptionist.staffId(), List.of(
                new PermissionChange(StaffPermission.VIEW_AGENDA, StaffPermissionOverrideState.INHERIT)));

        assertTrue(enabled(inheritedAgain, StaffPermission.VIEW_AGENDA));
        assertEquals(1, permissionRepository.overrides.size());
    }

    @Test
    void adminPermissionsAreAlwaysEnabledAndCannotBeOverridden() {
        UUID clinicId = UUID.randomUUID();
        ClinicStaff admin = staff(clinicId, userDirectory.addUser("root@clinerya.com", "Root"), StaffRole.ADMIN);
        staffRepository.save(admin);

        PermissionSummary summary = service.getPermissions(clinicId, admin.getId());

        assertTrue(summary.permissions().stream().allMatch(item -> item.enabled()));
        assertThrows(IllegalStateException.class, () -> service.updatePermissions(clinicId, admin.getId(), List.of(
                new PermissionChange(StaffPermission.VIEW_AGENDA, StaffPermissionOverrideState.REVOKED))));
    }

    private static boolean enabled(PermissionSummary summary, StaffPermission permission) {
        return summary.permissions().stream()
                .filter(item -> item.permission() == permission)
                .findFirst()
                .orElseThrow()
                .enabled();
    }

    private static ClinicStaff staff(UUID clinicId, UUID userId, StaffRole role) {
        return ClinicStaff.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .userId(userId)
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String emailFor(UUID userId) {
        return userDirectory.findUser(userId).orElseThrow().email();
    }

    private static final class InMemoryClinicStaffRepository implements ClinicStaffRepositoryPort {
        private final List<ClinicStaff> staff = new ArrayList<>();

        @Override
        public ClinicStaff save(ClinicStaff value) {
            staff.removeIf(existing -> existing.getId().equals(value.getId()));
            staff.add(value);
            return value;
        }

        @Override
        public Optional<ClinicStaff> findById(UUID id) {
            return staff.stream().filter(value -> id.equals(value.getId())).findFirst();
        }

        @Override
        public Optional<ClinicStaff> findByClinicIdAndUserId(UUID clinicId, UUID userId) {
            return staff.stream()
                    .filter(value -> clinicId.equals(value.getClinicId()))
                    .filter(value -> userId.equals(value.getUserId()))
                    .findFirst();
        }

        @Override
        public List<ClinicStaff> findByClinicId(UUID clinicId) {
            return staff.stream().filter(value -> clinicId.equals(value.getClinicId())).toList();
        }

        @Override
        public List<ClinicStaff> findByUserId(UUID userId) {
            return staff.stream().filter(value -> userId.equals(value.getUserId())).toList();
        }
    }

    private static final class InMemoryDoctorProfileRepository implements DoctorProfileRepositoryPort {
        private final List<DoctorProfile> profiles = new ArrayList<>();

        @Override
        public DoctorProfile save(DoctorProfile profile) {
            profiles.removeIf(existing -> existing.getId().equals(profile.getId()));
            profiles.add(profile);
            return profile;
        }

        @Override
        public Optional<DoctorProfile> findByClinicStaffId(UUID clinicStaffId) {
            return profiles.stream()
                    .filter(profile -> clinicStaffId.equals(profile.getClinicStaffId()))
                    .findFirst();
        }
    }

    private static final class InMemoryUserDirectory implements UserDirectoryPort {
        private final List<UserSummary> users = new ArrayList<>();

        UUID addUser(String email, String fullName) {
            UUID id = UUID.randomUUID();
            users.add(new UserSummary(id, fullName, email.trim().toLowerCase()));
            return id;
        }

        @Override
        public Optional<UserSummary> findUser(UUID userId) {
            return users.stream().filter(user -> userId.equals(user.id())).findFirst();
        }

        @Override
        public Optional<UserSummary> findByEmail(String email) {
            return users.stream()
                    .filter(user -> user.email().equals(email.trim().toLowerCase()))
                    .findFirst();
        }
    }

    private static final class InMemoryInvitationRepository implements ClinicStaffInvitationRepositoryPort {
        private final List<ClinicStaffInvitation> invitations = new ArrayList<>();

        @Override
        public ClinicStaffInvitation save(ClinicStaffInvitation invitation) {
            invitations.removeIf(existing -> existing.getId().equals(invitation.getId()));
            invitations.add(invitation);
            return invitation;
        }

        @Override
        public Optional<ClinicStaffInvitation> findByTokenAndUsedFalse(String token) {
            return invitations.stream()
                    .filter(invitation -> token.equals(invitation.getToken()))
                    .filter(invitation -> !invitation.isUsed())
                    .findFirst();
        }

        @Override
        public List<ClinicStaffInvitation> findByClinicIdAndUsedFalse(UUID clinicId) {
            return invitations.stream()
                    .filter(invitation -> clinicId.equals(invitation.getClinicId()))
                    .filter(invitation -> !invitation.isUsed())
                    .toList();
        }

        @Override
        public Optional<ClinicStaffInvitation> findByClinicIdAndEmailAndUsedFalse(UUID clinicId, String email) {
            return invitations.stream()
                    .filter(invitation -> clinicId.equals(invitation.getClinicId()))
                    .filter(invitation -> email.equals(invitation.getEmail()))
                    .filter(invitation -> !invitation.isUsed())
                    .findFirst();
        }
    }

    private static final class InMemoryPermissionOverrideRepository implements StaffPermissionOverrideRepositoryPort {
        private final List<StaffPermissionOverride> overrides = new ArrayList<>();

        @Override
        public StaffPermissionOverride save(StaffPermissionOverride override) {
            overrides.removeIf(existing -> existing.getId().equals(override.getId()));
            overrides.add(override);
            return override;
        }

        @Override
        public List<StaffPermissionOverride> findByClinicIdAndStaffId(UUID clinicId, UUID staffId) {
            return overrides.stream()
                    .filter(override -> clinicId.equals(override.getClinicId()))
                    .filter(override -> staffId.equals(override.getStaffId()))
                    .toList();
        }

        @Override
        public Optional<StaffPermissionOverride> findByClinicIdAndStaffIdAndPermission(UUID clinicId, UUID staffId, StaffPermission permission) {
            return overrides.stream()
                    .filter(override -> clinicId.equals(override.getClinicId()))
                    .filter(override -> staffId.equals(override.getStaffId()))
                    .filter(override -> permission == override.getPermission())
                    .findFirst();
        }

        @Override
        public void delete(StaffPermissionOverride override) {
            overrides.removeIf(existing -> existing.getId().equals(override.getId()));
        }
    }
}
