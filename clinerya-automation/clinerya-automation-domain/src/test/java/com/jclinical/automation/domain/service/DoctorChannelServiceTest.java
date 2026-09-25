package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.DoctorChannel;
import com.jclinical.automation.domain.ports.in.ManageDoctorChannelUseCase.DoctorChannelUpdate;
import com.jclinical.automation.domain.ports.in.ManageDoctorChannelUseCase.DoctorChannelView;
import com.jclinical.automation.domain.ports.out.DoctorChannelRepositoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort.DoctorContact;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El celular donde el medico recibe los avisos (D8). Lo registra el propio medico, o quien administra
 * las integraciones; sin su consentimiento no se activa.
 */
class DoctorChannelServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 10, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID doctorUserId = UUID.randomUUID();
    private final UUID otherDoctorId = UUID.randomUUID();
    private final UUID adminUserId = UUID.randomUUID();
    private final UUID receptionUserId = UUID.randomUUID();

    private final Map<UUID, UUID> staffOfUser = Map.of(doctorUserId, doctorId);
    private final Set<UUID> integrationManagers = Set.of(adminUserId);
    private final InMemoryChannels channels = new InMemoryChannels();

    @Test
    void aDoctorRegistersTheirOwnWhatsAppWithConsent() {
        DoctorChannelView view = serviceAt(NOW).updateMine(doctorUserId, clinicId,
                new DoctorChannelUpdate("55 1234 5678", true, true));

        assertEquals(doctorId, view.staffId());
        assertEquals("5215512345678", view.phone(), "10 digitos se guardan como los manda WhatsApp (521...)");
        assertTrue(view.active());
        assertEquals(NOW, view.consentAt());
        assertEquals(doctorUserId, channels.saved.get(doctorId).updatedBy());
    }

    @Test
    void numbersAreStoredTheWayWhatsAppReportsTheSender() {
        assertEquals("5215512345678", phoneAfterSaving("+52 1 55 1234 5678"));
        assertEquals("5215512345678", phoneAfterSaving("+52 (55) 1234-5678"));
        assertEquals("14155550100", phoneAfterSaving("+1 415 555 0100"));
    }

    @Test
    void anInvalidNumberIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> phoneAfterSaving("12345"));
        assertThrows(IllegalArgumentException.class, () -> phoneAfterSaving("55 abcd 5678"));
        assertThrows(IllegalArgumentException.class, () -> phoneAfterSaving(""));
        assertTrue(channels.saved.isEmpty());
    }

    @Test
    void noticesCannotBeTurnedOnWithoutTheDoctorsConsent() {
        assertThrows(IllegalArgumentException.class, () -> serviceAt(NOW).updateMine(doctorUserId, clinicId,
                new DoctorChannelUpdate("5512345678", false, true)));
        assertTrue(channels.saved.isEmpty());
    }

    @Test
    void withdrawingConsentTurnsTheNoticesOffButKeepsTheNumber() {
        serviceAt(NOW).updateMine(doctorUserId, clinicId, new DoctorChannelUpdate("5512345678", true, true));

        DoctorChannelView view = serviceAt(NOW.plusDays(1)).updateMine(doctorUserId, clinicId,
                new DoctorChannelUpdate("5512345678", false, false));

        assertFalse(view.active());
        assertNull(view.consentAt());
        assertEquals("5215512345678", view.phone());
    }

    @Test
    void theConsentDateIsKeptWhenOnlyTheNumberChanges() {
        serviceAt(NOW).updateMine(doctorUserId, clinicId, new DoctorChannelUpdate("5512345678", true, true));

        DoctorChannelView view = serviceAt(NOW.plusDays(3)).updateMine(doctorUserId, clinicId,
                new DoctorChannelUpdate("5587654321", true, true));

        assertEquals(NOW, view.consentAt());
        assertEquals("5215587654321", view.phone());
        assertEquals(NOW.plusDays(3), view.updatedAt());
    }

    @Test
    void anUnconfiguredDoctorSeesAnEmptyChannel() {
        DoctorChannelView view = serviceAt(NOW).getMine(doctorUserId, clinicId);

        assertEquals(doctorId, view.staffId());
        assertNull(view.phone());
        assertFalse(view.active());
    }

    @Test
    void staffWhoDoNotAttendPatientsHaveNoChannelOfTheirOwn() {
        assertThrows(ClinicAccessDeniedException.class, () -> serviceAt(NOW).getMine(receptionUserId, clinicId));
        assertThrows(ClinicAccessDeniedException.class, () -> serviceAt(NOW).updateMine(receptionUserId, clinicId,
                new DoctorChannelUpdate("5512345678", true, true)));
    }

    @Test
    void aDoctorCannotSeeOrChangeAnotherDoctorsNumber() {
        assertThrows(ClinicAccessDeniedException.class, () -> serviceAt(NOW).get(doctorUserId, clinicId, otherDoctorId));
        assertThrows(ClinicAccessDeniedException.class, () -> serviceAt(NOW).update(doctorUserId, clinicId, otherDoctorId,
                new DoctorChannelUpdate("5512345678", true, true)));
        assertTrue(channels.saved.isEmpty());
    }

    @Test
    void whoManagesIntegrationsConfiguresAnyDoctorOfTheClinic() {
        DoctorChannelView view = serviceAt(NOW).update(adminUserId, clinicId, otherDoctorId,
                new DoctorChannelUpdate("5512345678", true, true));

        assertEquals(otherDoctorId, view.staffId());
        assertEquals(adminUserId, channels.saved.get(otherDoctorId).updatedBy());
        assertEquals(view, serviceAt(NOW).get(adminUserId, clinicId, otherDoctorId));
    }

    @Test
    void onlyStaffWhoAttendPatientsCanHaveAChannel() {
        assertThrows(IllegalArgumentException.class, () -> serviceAt(NOW).update(adminUserId, clinicId, UUID.randomUUID(),
                new DoctorChannelUpdate("5512345678", true, true)));
    }

    // ---- utilidades -------------------------------------------------------------------------

    private String phoneAfterSaving(String raw) {
        return serviceAt(NOW).updateMine(doctorUserId, clinicId, new DoctorChannelUpdate(raw, true, true)).phone();
    }

    private DoctorChannelService serviceAt(LocalDateTime now) {
        return new DoctorChannelService(channels,
                (clinic, user) -> Optional.ofNullable(staffOfUser.get(user)),
                new FakeDoctors(),
                (clinic, user, permission) -> permission == StaffPermission.MANAGE_INTEGRATIONS && integrationManagers.contains(user),
                Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private final class FakeDoctors implements DoctorDirectoryPort {
        @Override
        public List<DoctorContact> listDoctors(UUID clinic) {
            return List.of(new DoctorContact(doctorId, "Dra. B"), new DoctorContact(otherDoctorId, "Dr. C"));
        }

        @Override
        public Optional<DoctorContact> lastDoctorOf(UUID clinic, UUID patientId) {
            return Optional.empty();
        }
    }

    static final class InMemoryChannels implements DoctorChannelRepositoryPort {
        final Map<UUID, DoctorChannel> saved = new HashMap<>();

        @Override
        public Optional<DoctorChannel> find(UUID clinicId, UUID staffId) {
            return Optional.ofNullable(saved.get(staffId)).filter(channel -> channel.clinicId().equals(clinicId));
        }

        @Override
        public Optional<DoctorChannel> findActiveByPhone(UUID clinicId, String phone) {
            return saved.values().stream()
                    .filter(channel -> channel.clinicId().equals(clinicId) && channel.active() && phone.equals(channel.phone()))
                    .findFirst();
        }

        @Override
        public DoctorChannel save(DoctorChannel channel) {
            saved.put(channel.staffId(), channel);
            return channel;
        }
    }
}
