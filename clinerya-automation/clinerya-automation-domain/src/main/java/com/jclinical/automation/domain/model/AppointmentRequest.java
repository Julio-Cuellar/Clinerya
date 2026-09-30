package com.jclinical.automation.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Solicitud de cita que un paciente hizo por chat y que solo el medico asignado puede resolver.
 * Inmutable: cada respuesta produce una copia nueva. {@code holdId} es el cupo apartado original;
 * {@code proposedOptions} los cupos que el medico propuso, cada uno con su propio apartado.
 */
public record AppointmentRequest(
        UUID id,
        UUID clinicId,
        UUID conversationId,
        UUID patientId,
        String patientName,
        String patientPhone,
        UUID doctorStaffId,
        String doctorName,
        LocalDateTime start,
        LocalDateTime end,
        UUID holdId,
        Status status,
        List<ProposedOption> proposedOptions,
        UUID appointmentId,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime respondedAt,
        /** Cita que esta solicitud reemplaza (reprogramar); null en una solicitud nueva. */
        UUID replacesAppointmentId,
        /** Servicio del catalogo de la cita (opcional). */
        UUID serviceId
) {
    public AppointmentRequest(UUID id, UUID clinicId, UUID conversationId, UUID patientId, String patientName,
                              String patientPhone, UUID doctorStaffId, String doctorName, LocalDateTime start,
                              LocalDateTime end, UUID holdId, Status status, List<ProposedOption> proposedOptions,
                              UUID appointmentId, String rejectionReason, LocalDateTime createdAt, LocalDateTime respondedAt,
                              UUID replacesAppointmentId) {
        this(id, clinicId, conversationId, patientId, patientName, patientPhone, doctorStaffId, doctorName, start, end, holdId,
                status, proposedOptions, appointmentId, rejectionReason, createdAt, respondedAt, replacesAppointmentId, null);
    }

    public AppointmentRequest(UUID id, UUID clinicId, UUID conversationId, UUID patientId, String patientName,
                              String patientPhone, UUID doctorStaffId, String doctorName, LocalDateTime start,
                              LocalDateTime end, UUID holdId, Status status, List<ProposedOption> proposedOptions,
                              UUID appointmentId, String rejectionReason, LocalDateTime createdAt, LocalDateTime respondedAt) {
        this(id, clinicId, conversationId, patientId, patientName, patientPhone, doctorStaffId, doctorName, start, end, holdId,
                status, proposedOptions, appointmentId, rejectionReason, createdAt, respondedAt, null, null);
    }


    /** Plazo del medico para responder y del paciente para elegir una opcion propuesta. */
    public static final Duration RESPONSE_WINDOW = Duration.ofHours(24);

    public enum Status {
        /** Esperando al medico. */
        PENDING,
        /** El medico propuso otros horarios; esperando al paciente. */
        OPTIONS_PROPOSED,
        BOOKED,
        REJECTED,
        /** El paciente no quiso ninguna de las opciones propuestas. */
        DECLINED,
        EXPIRED
    }

    public record ProposedOption(UUID holdId, LocalDateTime start, LocalDateTime end) {}

    public AppointmentRequest {
        proposedOptions = proposedOptions == null ? List.of() : List.copyOf(proposedOptions);
    }

    public boolean isOverdueAt(LocalDateTime now) {
        return switch (status) {
            case PENDING -> !createdAt.plus(RESPONSE_WINDOW).isAfter(now);
            case OPTIONS_PROPOSED -> !respondedAt.plus(RESPONSE_WINDOW).isAfter(now);
            default -> false;
        };
    }

    public AppointmentRequest booked(UUID newAppointmentId, LocalDateTime now) {
        return copy(Status.BOOKED, proposedOptions, newAppointmentId, rejectionReason, respondedAt == null ? now : respondedAt);
    }

    public AppointmentRequest rejected(String reason, LocalDateTime now) {
        return copy(Status.REJECTED, proposedOptions, appointmentId, reason, now);
    }

    public AppointmentRequest optionsProposed(List<ProposedOption> options, LocalDateTime now) {
        return copy(Status.OPTIONS_PROPOSED, options, appointmentId, rejectionReason, now);
    }

    public AppointmentRequest declined() {
        return copy(Status.DECLINED, proposedOptions, appointmentId, rejectionReason, respondedAt);
    }

    public AppointmentRequest expired() {
        return copy(Status.EXPIRED, proposedOptions, appointmentId, rejectionReason, respondedAt);
    }

    private AppointmentRequest copy(Status newStatus, List<ProposedOption> options, UUID newAppointmentId,
                                    String reason, LocalDateTime newRespondedAt) {
        return new AppointmentRequest(id, clinicId, conversationId, patientId, patientName, patientPhone,
                doctorStaffId, doctorName, start, end, holdId, newStatus, options, newAppointmentId, reason,
                createdAt, newRespondedAt, replacesAppointmentId, serviceId);
    }
}
