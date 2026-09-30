package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AppointmentReminder;
import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.ports.out.AppointmentReminderRepositoryPort;
import com.jclinical.automation.domain.ports.out.ReminderPatientPort.ReminderRecipient;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakeDoctors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan v2, S6: recordatorio de cita por WhatsApp. Se programa al agendarse la cita (evento), se mueve si
 * se reprograma y se quita si se cancela. Sale una sola vez, solo a pacientes que autorizaron WhatsApp y
 * si la clinica lo tiene encendido, con botones para confirmar, cancelar o reprogramar.
 */
class AppointmentReminderServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 9, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();
    private final InMemoryReminders reminders = new InMemoryReminders();
    private final Map<UUID, ReminderRecipient> recipients = new HashMap<>();
    private final FakeDoctors doctors = new FakeDoctors();
    private final List<PatientNotification> sent = new ArrayList<>();
    private AssistantProfile profile = AssistantProfile.EMPTY;

    private AppointmentReminderService service;

    @BeforeEach
    void setUp() {
        doctors.add(doctorId, "Dra. Ramírez");
        recipients.put(patientId, new ReminderRecipient("Ana López", "5215512345678", true));
        service = new AppointmentReminderService(reminders, clinic -> profile,
                (clinic, patient) -> Optional.ofNullable(recipients.get(patient)), doctors, clinic -> Optional.empty(),
                sent::add, Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test
    void anAppointmentGetsItsReminderTwentyFourHoursBeforeByDefault() {
        LocalDateTime start = NOW.plusDays(3).withHour(16);

        service.appointmentScheduled(clinicId, appointmentId, patientId, doctorId, start, "Limpieza dental");

        AppointmentReminder reminder = reminders.byId.get(appointmentId);
        assertEquals(start.minusHours(24), reminder.sendAt());
        assertEquals("Limpieza dental", reminder.serviceName());
    }

    @Test
    void theClinicChoosesHowManyHoursBefore() {
        profile = new AssistantProfile(null, null, true, true, 48, "recordatorio_cita");
        LocalDateTime start = NOW.plusDays(5);

        service.appointmentScheduled(clinicId, appointmentId, patientId, doctorId, start, null);

        assertEquals(start.minusHours(48), reminders.byId.get(appointmentId).sendAt());
    }

    @Test
    void aSoonAppointmentIsRemindedNowButNotIfItIsAboutToStart() {
        UUID soon = UUID.randomUUID();
        UUID imminent = UUID.randomUUID();

        service.appointmentScheduled(clinicId, soon, patientId, doctorId, NOW.plusHours(5), null);
        service.appointmentScheduled(clinicId, imminent, patientId, doctorId, NOW.plusMinutes(90), null);
        service.appointmentScheduled(clinicId, UUID.randomUUID(), null, doctorId, NOW.plusDays(3), null);

        assertEquals(NOW, reminders.byId.get(soon).sendAt());
        assertFalse(reminders.byId.containsKey(imminent), "a menos de 2 h ya no tiene caso");
        assertEquals(1, reminders.byId.size(), "sin paciente no hay a quien recordar");
    }

    @Test
    void reschedulingMovesTheReminderKeepsTheServiceAndCancellingRemovesIt() {
        service.appointmentScheduled(clinicId, appointmentId, patientId, doctorId, NOW.plusDays(3), "Limpieza dental");
        reminders.markSent(appointmentId, NOW);
        LocalDateTime newStart = NOW.plusDays(6);

        service.appointmentRescheduled(clinicId, appointmentId, patientId, doctorId, newStart);
        AppointmentReminder moved = reminders.byId.get(appointmentId);
        assertEquals(newStart.minusHours(24), moved.sendAt());
        assertEquals("Limpieza dental", moved.serviceName());
        assertNull(moved.sentAt(), "la nueva fecha se vuelve a recordar");

        service.appointmentCancelled(appointmentId);
        assertTrue(reminders.byId.isEmpty());
    }

    @Test
    void dueRemindersAreSentOnceWithButtonsToConfirmCancelOrReschedule() {
        profile = new AssistantProfile(null, null, true, true, 24, "recordatorio_cita");
        service.appointmentScheduled(clinicId, appointmentId, patientId, doctorId, NOW.plusHours(24), "Limpieza dental");

        assertEquals(1, service.sendDue());
        assertEquals(0, service.sendDue(), "no sale dos veces");

        PatientNotification notification = sent.getFirst();
        assertEquals("5215512345678", notification.phone());
        assertEquals("recordatorio_cita", notification.templateName());
        String text = notification.reply().text();
        assertTrue(text.contains("Ana") && text.contains("Dra. Ramírez") && text.contains("Limpieza dental"), text);
        assertEquals(List.of(AppointmentReminderService.CONFIRM + appointmentId, AppointmentReminderService.CANCEL + appointmentId,
                AppointmentReminderService.RESCHEDULE + appointmentId), notification.reply().options().stream()
                .map(ConversationOption::id).toList());
        assertEquals(List.of("Ana", "la clínica", SlotLabel.of(NOW.plusHours(24)), "Dra. Ramírez"),
                notification.templateParameters());
    }

    @Test
    void withoutConsentOrWithRemindersOffNothingIsSent() {
        recipients.put(patientId, new ReminderRecipient("Ana López", "5215512345678", false));
        service.appointmentScheduled(clinicId, appointmentId, patientId, doctorId, NOW.plusHours(10), null);
        assertEquals(0, service.sendDue());

        recipients.put(patientId, new ReminderRecipient("Ana López", "5215512345678", true));
        profile = new AssistantProfile(null, null, true, false, 24, null);
        service.appointmentScheduled(clinicId, appointmentId, patientId, doctorId, NOW.plusHours(10), null);
        assertEquals(0, service.sendDue());
        assertTrue(sent.isEmpty());
        assertTrue(reminders.findDue(NOW, 10).isEmpty(), "no se queda reintentando");
    }

    static final class InMemoryReminders implements AppointmentReminderRepositoryPort {
        final Map<UUID, AppointmentReminder> byId = new HashMap<>();

        @Override
        public Optional<AppointmentReminder> find(UUID appointmentId) {
            return Optional.ofNullable(byId.get(appointmentId));
        }

        @Override
        public void save(AppointmentReminder reminder) {
            byId.put(reminder.appointmentId(), reminder);
        }

        @Override
        public void delete(UUID appointmentId) {
            byId.remove(appointmentId);
        }

        @Override
        public List<AppointmentReminder> findDue(LocalDateTime now, int limit) {
            return byId.values().stream().filter(r -> r.sentAt() == null && !r.sendAt().isAfter(now))
                    .sorted(Comparator.comparing(AppointmentReminder::sendAt)).limit(limit).toList();
        }

        @Override
        public boolean markSent(UUID appointmentId, LocalDateTime at) {
            AppointmentReminder found = byId.get(appointmentId);
            if (found == null || found.sentAt() != null) {
                return false;
            }
            byId.put(appointmentId, found.sent(at));
            return true;
        }
    }
}
