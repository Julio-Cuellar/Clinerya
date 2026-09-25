package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AppointmentRequest;
import com.jclinical.automation.domain.model.AppointmentRequest.Status;
import com.jclinical.automation.domain.model.DoctorChannel;
import com.jclinical.automation.domain.model.DoctorNotice;
import com.jclinical.automation.domain.ports.out.PendingRequestReminderPort;
import com.jclinical.automation.domain.service.DoctorChannelServiceTest.InMemoryChannels;
import com.jclinical.automation.domain.service.NotifyingChatHistoryTest.RecordingNotifier;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El medico solo recibe avisos por WhatsApp y responde en Clinerya (D8). El aviso no lleva datos del
 * paciente: solo la fecha y el enlace a la bandeja.
 */
class DoctorNotificationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 28, 14, 0);
    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 29, 10, 0);
    private static final String INBOX = "https://app.clinerya.test/solicitudes-de-cita";
    private static final String DOCTOR_PHONE = "5215599990000";

    private final UUID clinicId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final InMemoryChannels channels = new InMemoryChannels();
    private final List<DoctorNotice> notices = new ArrayList<>();
    private final FakeReminders reminders = new FakeReminders();
    private final RecordingNotifier realtime = new RecordingNotifier();

    private final DoctorNotificationService service = new DoctorNotificationService(channels, notices::add, reminders,
            realtime, INBOX, Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    @Test
    void aNewRequestSendsTheDoctorANoticeWithTheLinkToTheInbox() {
        activeChannel();

        service.newRequest(request(NOW.minusMinutes(1)));

        assertEquals(1, notices.size());
        DoctorNotice notice = notices.get(0);
        assertEquals(clinicId, notice.clinicId());
        assertEquals(DOCTOR_PHONE, notice.phone());
        assertTrue(notice.text().contains("Mar 29/09 10:00"), notice.text());
        assertTrue(notice.text().contains(INBOX), notice.text());
        assertEquals(2, notice.templateParameters().size(), "la plantilla aprobada lleva el resumen y el enlace");
        assertTrue(notice.templateParameters().get(0).contains("Mar 29/09 10:00"));
        assertEquals(INBOX, notice.templateParameters().get(1));
    }

    @Test
    void theDoctorsOpenInboxIsUpdatedEvenWithoutWhatsApp() {
        AppointmentRequest request = request(NOW);

        service.newRequest(request);

        assertTrue(notices.isEmpty());
        assertEquals(List.of(clinicId + "|" + doctorId + "|" + request.id()), realtime.newRequests);
    }

    @Test
    void theNoticeCarriesNoPatientData() {
        activeChannel();

        service.newRequest(request(NOW));

        String everything = notices.get(0).text() + notices.get(0).templateParameters();
        assertFalse(everything.contains("Ana"), everything);
        assertFalse(everything.contains("5215512345678"), everything);
    }

    @Test
    void doctorsWithoutAnActiveChannelAreNotNotified() {
        service.newRequest(request(NOW));
        channels.save(new DoctorChannel(clinicId, doctorId, DOCTOR_PHONE, false, null, null, NOW));
        service.newRequest(request(NOW));

        assertTrue(notices.isEmpty());
    }

    @Test
    void requestsStillPendingAfterFourHoursAreRemindedOnce() {
        activeChannel();
        AppointmentRequest waiting = request(NOW.minusHours(5));
        reminders.due.add(waiting);

        int reminded = service.remindPending();

        assertEquals(1, reminded);
        assertEquals(NOW.minusHours(4), reminders.cutoff, "solo las que llevan 4 h o mas sin respuesta");
        assertEquals(List.of(waiting.id()), reminders.marked);
        assertTrue(notices.get(0).text().startsWith("Recordatorio"), notices.get(0).text());
        assertTrue(notices.get(0).text().contains(INBOX));
    }

    @Test
    void remindersForDoctorsWithoutAChannelAreMarkedSoTheyAreNotRetried() {
        AppointmentRequest waiting = request(NOW.minusHours(5));
        reminders.due.add(waiting);

        assertEquals(0, service.remindPending());
        assertEquals(List.of(waiting.id()), reminders.marked);
        assertTrue(notices.isEmpty());
    }

    @Test
    void aDoctorWhoWritesOnWhatsAppIsToldToAnswerInClinerya() {
        activeChannel();

        assertTrue(service.replyIfDoctor(clinicId, DOCTOR_PHONE));
        assertEquals(1, notices.size());
        assertTrue(notices.get(0).text().contains("Clinerya"), notices.get(0).text());
        assertTrue(notices.get(0).text().contains(INBOX));

        assertFalse(service.replyIfDoctor(clinicId, "5215512345678"), "un paciente sigue su conversacion");
        assertFalse(service.replyIfDoctor(UUID.randomUUID(), DOCTOR_PHONE), "el numero es del medico solo en su clinica");
        assertEquals(1, notices.size());
    }

    // ---- utilidades -------------------------------------------------------------------------

    private void activeChannel() {
        channels.save(new DoctorChannel(clinicId, doctorId, DOCTOR_PHONE, true, NOW.minusDays(1), null, NOW.minusDays(1)));
    }

    private AppointmentRequest request(LocalDateTime createdAt) {
        return new AppointmentRequest(UUID.randomUUID(), clinicId, UUID.randomUUID(), UUID.randomUUID(), "Ana López",
                "5215512345678", doctorId, "Dra. B", START, START.plusMinutes(30), UUID.randomUUID(), Status.PENDING,
                List.of(), null, null, createdAt, null);
    }

    static final class FakeReminders implements PendingRequestReminderPort {
        final List<AppointmentRequest> due = new ArrayList<>();
        final List<UUID> marked = new ArrayList<>();
        LocalDateTime cutoff;

        @Override
        public List<AppointmentRequest> findPendingNotRemindedBefore(LocalDateTime createdBefore) {
            cutoff = createdBefore;
            return due;
        }

        @Override
        public void markReminded(UUID requestId, LocalDateTime at) {
            marked.add(requestId);
        }
    }
}
