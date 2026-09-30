package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AppointmentReminder;
import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.model.ClinicInfo;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.ports.in.AppointmentRemindersUseCase;
import com.jclinical.automation.domain.ports.out.AppointmentReminderRepositoryPort;
import com.jclinical.automation.domain.ports.out.AssistantProfilePort;
import com.jclinical.automation.domain.ports.out.ClinicInfoPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort.DoctorContact;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;
import com.jclinical.automation.domain.ports.out.ReminderPatientPort;
import com.jclinical.automation.domain.ports.out.ReminderPatientPort.ReminderRecipient;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Recordatorio de cita por WhatsApp (plan v2, S6). Los eventos de la agenda lo programan, lo mueven o lo
 * quitan; un barrido lo envia una sola vez, solo si la clinica lo tiene encendido y el paciente autorizo
 * WhatsApp. Lleva botones para confirmar, cancelar o reprogramar: el id de cada uno trae la cita.
 */
public class AppointmentReminderService implements AppointmentRemindersUseCase {

    public static final String CONFIRM = "recordatorio:confirmar:";
    public static final String CANCEL = "recordatorio:cancelar:";
    public static final String RESCHEDULE = "recordatorio:reprogramar:";
    /** Si la cita empieza antes de esto ya no tiene caso recordarla. */
    static final Duration MINIMUM_NOTICE = Duration.ofHours(2);
    static final int BATCH_SIZE = 50;
    private static final String DEFAULT_CLINIC = "la clínica";

    private final AppointmentReminderRepositoryPort reminders;
    private final AssistantProfilePort profiles;
    private final ReminderPatientPort patients;
    private final DoctorDirectoryPort doctors;
    private final ClinicInfoPort clinics;
    private final OutboundMessageQueuePort outbound;
    private final Clock clock;

    public AppointmentReminderService(AppointmentReminderRepositoryPort reminders, AssistantProfilePort profiles,
                                      ReminderPatientPort patients, DoctorDirectoryPort doctors, ClinicInfoPort clinics,
                                      OutboundMessageQueuePort outbound, Clock clock) {
        this.reminders = reminders;
        this.profiles = profiles;
        this.patients = patients;
        this.doctors = doctors;
        this.clinics = clinics;
        this.outbound = outbound;
        this.clock = clock;
    }

    @Override
    public void appointmentScheduled(UUID clinicId, UUID appointmentId, UUID patientId, UUID doctorStaffId,
                                     LocalDateTime startsAt, String serviceName) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (patientId == null || startsAt == null || startsAt.isBefore(now.plus(MINIMUM_NOTICE))) {
            reminders.delete(appointmentId);
            return;
        }
        LocalDateTime sendAt = startsAt.minusHours(profiles.find(clinicId).reminderHoursBefore());
        reminders.save(new AppointmentReminder(appointmentId, clinicId, patientId, doctorStaffId, startsAt, serviceName,
                sendAt.isBefore(now) ? now : sendAt, null));
    }

    @Override
    public void appointmentRescheduled(UUID clinicId, UUID appointmentId, UUID patientId, UUID doctorStaffId,
                                       LocalDateTime newStart) {
        String serviceName = reminders.find(appointmentId).map(AppointmentReminder::serviceName).orElse(null);
        appointmentScheduled(clinicId, appointmentId, patientId, doctorStaffId, newStart, serviceName);
    }

    @Override
    public void appointmentCancelled(UUID appointmentId) {
        reminders.delete(appointmentId);
    }

    @Override
    public int sendDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        int sent = 0;
        for (AppointmentReminder reminder : reminders.findDue(now, BATCH_SIZE)) {
            Optional<PatientNotification> notification = notificationFor(reminder, now);
            if (notification.isEmpty()) {
                reminders.delete(reminder.appointmentId());
            } else if (reminders.markSent(reminder.appointmentId(), now)) {
                outbound.enqueue(notification.get());
                sent++;
            }
        }
        return sent;
    }

    private Optional<PatientNotification> notificationFor(AppointmentReminder reminder, LocalDateTime now) {
        AssistantProfile profile = profiles.find(reminder.clinicId());
        if (!profile.remindersEnabled() || !reminder.startsAt().isAfter(now)) {
            return Optional.empty();
        }
        return patients.find(reminder.clinicId(), reminder.patientId())
                .filter(recipient -> recipient.whatsappConsent() && recipient.phone() != null && !recipient.phone().isBlank())
                .map(recipient -> notification(reminder, recipient, profile));
    }

    private PatientNotification notification(AppointmentReminder reminder, ReminderRecipient recipient,
                                             AssistantProfile profile) {
        String firstName = firstName(recipient.fullName());
        String clinic = clinics.find(reminder.clinicId()).map(ClinicInfo::name).filter(name -> !name.isBlank())
                .orElse(DEFAULT_CLINIC);
        String when = SlotLabel.of(reminder.startsAt());
        String doctor = doctors.listDoctors(reminder.clinicId()).stream()
                .filter(contact -> contact.staffId().equals(reminder.doctorStaffId()))
                .map(DoctorContact::displayName)
                .findFirst()
                .orElse("tu médico");
        String service = reminder.serviceName() == null || reminder.serviceName().isBlank()
                ? "" : " (" + reminder.serviceName() + ")";
        String text = "Hola " + firstName + ", te recordamos tu cita en " + clinic + " el " + when + " con " + doctor
                + service + ". ¿Nos confirmas tu asistencia?";
        UUID id = reminder.appointmentId();
        OutboundReply reply = new OutboundReply(text, List.of(
                new ConversationOption(CONFIRM + id, "Confirmo"),
                new ConversationOption(CANCEL + id, "Cancelar"),
                new ConversationOption(RESCHEDULE + id, "Reprogramar")));
        return new PatientNotification(reminder.clinicId(), recipient.phone(), reply, null,
                profile.reminderTemplateName(), List.of(firstName, clinic, when, doctor));
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        return fullName.strip().split("\\s+")[0];
    }
}
