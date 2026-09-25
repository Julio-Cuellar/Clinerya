package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.ports.in.ManageOnlineBookingSettingsUseCase.OnlineBookingSettings;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsPort;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsRepositoryPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reglas de la reserva en linea (decisiones de la entrega 3): la clinica define cuanto dura cada cupo
 * (30 min por defecto) y cada medico su anticipacion minima (2 h por defecto). El medico puede cambiar
 * la suya; la duracion y la anticipacion de otros solo quien administra la agenda.
 */
class OnlineBookingSettingsServiceTest {

    private final UUID clinicId = UUID.randomUUID();
    private final UUID doctorStaffId = UUID.randomUUID();
    private final UUID doctorUserId = UUID.randomUUID();
    private final UUID otherDoctorStaffId = UUID.randomUUID();
    private final UUID adminUserId = UUID.randomUUID();
    private final UUID receptionistUserId = UUID.randomUUID();

    private final InMemorySettings repository = new InMemorySettings();
    private final StaffValidatorPort staff = mock(StaffValidatorPort.class);
    private final Map<UUID, Set<StaffPermission>> permissions = new HashMap<>();
    private final StaffPermissionCheckerPort permissionChecker =
            (clinic, user, permission) -> permissions.getOrDefault(user, Set.of()).contains(permission);

    private OnlineBookingSettingsService service;

    @BeforeEach
    void setUp() {
        service = new OnlineBookingSettingsService(repository, staff, permissionChecker);
        permissions.put(adminUserId, Set.of(StaffPermission.MANAGE_AGENDA, StaffPermission.VIEW_AGENDA));
        permissions.put(doctorUserId, Set.of(StaffPermission.VIEW_AGENDA));
        permissions.put(receptionistUserId, Set.of(StaffPermission.VIEW_AGENDA));
        when(staff.findActiveDoctor(doctorStaffId, clinicId)).thenReturn(Optional.of(new DoctorSnapshot(doctorStaffId, "Dra. B")));
        when(staff.findActiveDoctor(otherDoctorStaffId, clinicId)).thenReturn(Optional.of(new DoctorSnapshot(otherDoctorStaffId, "Dr. C")));
        when(staff.listActiveDoctors(clinicId)).thenReturn(List.of(
                new DoctorSnapshot(doctorStaffId, "Dra. B"), new DoctorSnapshot(otherDoctorStaffId, "Dr. C")));
        when(staff.userIdOfStaff(doctorStaffId, clinicId)).thenReturn(Optional.of(doctorUserId));
    }

    @Test
    void withoutConfigurationTheDefaultsApply() {
        OnlineBookingSettingsPort rules = service;

        assertEquals(30, rules.slotMinutes(clinicId));
        assertEquals(120, rules.minLeadMinutes(clinicId, doctorStaffId));
    }

    @Test
    void theAgendaAdministratorSetsTheSlotLength() {
        service.updateSlotMinutes(adminUserId, clinicId, 45);

        assertEquals(45, service.slotMinutes(clinicId));
    }

    @Test
    void theSlotLengthMustBeReasonable() {
        assertThrows(IllegalArgumentException.class, () -> service.updateSlotMinutes(adminUserId, clinicId, 5));
        assertThrows(IllegalArgumentException.class, () -> service.updateSlotMinutes(adminUserId, clinicId, 300));
        assertThrows(IllegalArgumentException.class, () -> service.updateSlotMinutes(adminUserId, clinicId, 32));
    }

    @Test
    void someoneWhoDoesNotManageTheAgendaCannotChangeTheSlotLength() {
        assertThrows(ClinicAccessDeniedException.class, () -> service.updateSlotMinutes(receptionistUserId, clinicId, 45));
    }

    @Test
    void aDoctorSetsTheirOwnMinimumLeadTime() {
        service.updateDoctorLeadMinutes(doctorUserId, clinicId, doctorStaffId, 24 * 60);

        assertEquals(24 * 60, service.minLeadMinutes(clinicId, doctorStaffId));
        assertEquals(120, service.minLeadMinutes(clinicId, otherDoctorStaffId), "no afecta a otros medicos");
    }

    @Test
    void aDoctorCannotChangeAnotherDoctorsLeadTime() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.updateDoctorLeadMinutes(doctorUserId, clinicId, otherDoctorStaffId, 60));
    }

    @Test
    void theAgendaAdministratorCanSetAnyDoctorsLeadTime() {
        service.updateDoctorLeadMinutes(adminUserId, clinicId, otherDoctorStaffId, 0);

        assertEquals(0, service.minLeadMinutes(clinicId, otherDoctorStaffId));
    }

    @Test
    void theLeadTimeMustBeBetweenZeroAndAWeek() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateDoctorLeadMinutes(adminUserId, clinicId, doctorStaffId, -1));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateDoctorLeadMinutes(adminUserId, clinicId, doctorStaffId, 7 * 24 * 60 + 1));
    }

    @Test
    void theSettingsShowTheSlotLengthAndEveryDoctorsLeadTime() {
        service.updateDoctorLeadMinutes(doctorUserId, clinicId, doctorStaffId, 60);

        OnlineBookingSettings settings = service.getSettings(receptionistUserId, clinicId);

        assertEquals(30, settings.slotMinutes());
        assertEquals(2, settings.doctors().size());
        assertEquals(60, settings.doctors().get(0).minLeadMinutes());
        assertEquals(120, settings.doctors().get(1).minLeadMinutes());
    }

    static final class InMemorySettings implements OnlineBookingSettingsRepositoryPort {
        final Map<UUID, Integer> slotMinutes = new HashMap<>();
        final Map<String, Integer> leadMinutes = new HashMap<>();

        @Override
        public Optional<Integer> findSlotMinutes(UUID clinicId) {
            return Optional.ofNullable(slotMinutes.get(clinicId));
        }

        @Override
        public void saveSlotMinutes(UUID clinicId, int minutes) {
            slotMinutes.put(clinicId, minutes);
        }

        @Override
        public Optional<Integer> findLeadMinutes(UUID clinicId, UUID doctorStaffId) {
            return Optional.ofNullable(leadMinutes.get(clinicId + "/" + doctorStaffId));
        }

        @Override
        public void saveLeadMinutes(UUID clinicId, UUID doctorStaffId, int minutes) {
            leadMinutes.put(clinicId + "/" + doctorStaffId, minutes);
        }
    }
}
