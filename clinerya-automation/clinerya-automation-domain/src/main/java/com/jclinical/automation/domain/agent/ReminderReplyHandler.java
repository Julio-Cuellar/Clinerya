package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.agent.tools.ConfirmActionTool;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.AppointmentConfirmationPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort.DoctorContact;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort.UpcomingVisit;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;
import com.jclinical.automation.domain.ports.out.PendingActionPort;
import com.jclinical.automation.domain.service.AppointmentReminderService;
import com.jclinical.automation.domain.service.SlotLabel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que toca el paciente en el recordatorio de su cita (plan v2, S6). Solo cuenta si la cita es proxima
 * y de alguno de los pacientes de ese celular. Confirmo confirma sin pasar por el modelo; Cancelar deja la
 * cancelacion lista para que la confirme (igual que si la pidiera en el chat); Reprogramar la pasa al agente.
 */
public final class ReminderReplyHandler {

    static final String NOT_CURRENT = "Esa cita ya no está vigente. Si necesitas algo más, escríbeme por aquí.";
    private static final int LOOKUP_LIMIT = 10;

    /** Respuesta directa al paciente, o el mensaje que se le pasa al agente en lugar del boton. */
    public sealed interface Outcome {
        record Reply(OutboundReply reply) implements Outcome {}

        record ForAgent(String text) implements Outcome {}
    }

    private final PatientAppointmentsPort appointments;
    private final DoctorDirectoryPort doctors;
    private final PendingActionPort pending;
    private final AppointmentConfirmationPort confirmation;

    public ReminderReplyHandler(PatientAppointmentsPort appointments, DoctorDirectoryPort doctors, PendingActionPort pending,
                                AppointmentConfirmationPort confirmation) {
        this.appointments = appointments;
        this.doctors = doctors;
        this.pending = pending;
        this.confirmation = confirmation;
    }

    /** @return vacio si el boton no es de un recordatorio */
    public Optional<Outcome> handle(UUID clinicId, UUID conversationId, List<PatientContact> contacts, String optionId,
                                    LocalDateTime now) {
        if (optionId == null) {
            return Optional.empty();
        }
        String prefix = prefixOf(optionId);
        if (prefix == null) {
            return Optional.empty();
        }
        Optional<Found> found = find(clinicId, contacts, optionId.substring(prefix.length()));
        if (found.isEmpty()) {
            return Optional.of(new Outcome.Reply(OutboundReply.text(NOT_CURRENT)));
        }
        Found appointment = found.get();
        String label = SlotLabel.of(appointment.visit().start()) + " con " + appointment.doctorName();
        return Optional.of(switch (prefix) {
            case AppointmentReminderService.CONFIRM -> confirm(clinicId, appointment, label);
            case AppointmentReminderService.CANCEL -> proposeCancellation(conversationId, appointment, label, now);
            default -> new Outcome.ForAgent("Quiero reprogramar mi cita del " + label + " [cita:"
                    + appointment.visit().appointmentId() + "]");
        });
    }

    private Outcome confirm(UUID clinicId, Found appointment, String label) {
        boolean confirmed = confirmation.confirm(clinicId, appointment.visit().appointmentId(), appointment.patient().patientId());
        String text = confirmed
                ? "¡Gracias! Tu cita del " + label + " quedó confirmada. Te esperamos."
                : "Lo siento, no pude confirmarla: la agenda ya no lo permite. Si necesitas algo, escríbeme por aquí.";
        return new Outcome.Reply(OutboundReply.text(text));
    }

    private Outcome proposeCancellation(UUID conversationId, Found appointment, String label, LocalDateTime now) {
        UpcomingVisit visit = appointment.visit();
        pending.save(new PendingAction(conversationId, PendingAction.Kind.CANCEL, appointment.patient().patientId(),
                appointment.patient().displayName(), visit.doctorStaffId(), appointment.doctorName(), visit.start(), visit.end(),
                visit.appointmentId(), now));
        return new Outcome.Reply(new OutboundReply("¿Confirmas que quieres cancelar tu cita del " + label + "?",
                ConfirmActionTool.CONFIRMATION_OPTIONS));
    }

    private Optional<Found> find(UUID clinicId, List<PatientContact> contacts, String appointmentId) {
        for (PatientContact patient : contacts) {
            for (UpcomingVisit visit : appointments.upcoming(clinicId, patient.patientId(), LOOKUP_LIMIT)) {
                if (visit.appointmentId().toString().equals(appointmentId)) {
                    String doctor = doctors.listDoctors(clinicId).stream()
                            .filter(d -> d.staffId().equals(visit.doctorStaffId()))
                            .map(DoctorContact::displayName).findFirst().orElse("tu médico");
                    return Optional.of(new Found(patient, visit, doctor));
                }
            }
        }
        return Optional.empty();
    }

    private static String prefixOf(String optionId) {
        for (String prefix : List.of(AppointmentReminderService.CONFIRM, AppointmentReminderService.CANCEL,
                AppointmentReminderService.RESCHEDULE)) {
            if (optionId.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    private record Found(PatientContact patient, UpcomingVisit visit, String doctorName) {}
}
