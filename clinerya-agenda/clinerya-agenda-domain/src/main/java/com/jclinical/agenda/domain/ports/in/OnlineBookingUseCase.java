package com.jclinical.agenda.domain.ports.in;

import com.jclinical.agenda.domain.model.BookableSlot;
import com.jclinical.agenda.domain.model.SlotHold;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reserva en linea: cupos libres de un medico y apartado temporal mientras el medico decide.
 * Es una ruta interna (la usa la automatizacion); no recibe usuario porque quien pide es el paciente.
 */
public interface OnlineBookingUseCase {

    List<BookableSlot> findAvailableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit);

    /**
     * Cupos donde cabe un servicio de {@code durationMinutes}: el inicio sigue la rejilla de la clinica y
     * el cupo dura lo que el servicio. Sin duracion, el largo del cupo de la clinica.
     */
    default List<BookableSlot> findAvailableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit,
                                                  Integer durationMinutes) {
        return findAvailableSlots(clinicId, doctorStaffId, from, days, limit);
    }

    /** @throws SlotUnavailableException si el cupo ya no cumple las reglas o se ocupo. */
    SlotHold holdSlot(HoldSlotCommand command);

    void releaseHold(UUID clinicId, UUID holdId);

    /**
     * Convierte un apartado vigente en cita, con todas las validaciones de la agenda. Lo invoca la
     * automatizacion cuando el medico ya aprobo; el permiso del medico lo verifica quien llama.
     *
     * @return id de la cita creada.
     * @throws SlotUnavailableException si el apartado ya no esta vigente o la agenda rechaza la cita.
     */
    UUID bookHeldSlot(BookHeldSlotCommand command);

    /** Miembro del personal activo que corresponde al usuario en la clinica. */
    Optional<UUID> doctorStaffIdOfUser(UUID clinicId, UUID userId);

    /**
     * Proximas citas vigentes (programadas o confirmadas) del paciente, la mas proxima primero. Ruta
     * interna: quien pregunta es el paciente por WhatsApp, no un usuario del personal.
     */
    default List<UpcomingAppointment> upcomingAppointments(UUID clinicId, UUID patientId, int limit) {
        throw new UnsupportedOperationException();
    }

    /**
     * El paciente cancela su propia cita (vigente y futura). Ruta interna sin usuario del personal: la
     * cancelacion libera materiales, sincroniza el calendario y publica el evento como cualquier otra.
     *
     * @throws AppointmentNotCancellableException si la cita no es suya, ya paso o ya no esta vigente.
     */
    default void cancelByPatient(UUID clinicId, UUID appointmentId, UUID patientId, String reason) {
        throw new UnsupportedOperationException();
    }

    /**
     * Resumen de visitas del paciente para reconocerlo en Chats: proxima cita vigente, ultima atendida y
     * cuantas atendio, cancelo o falto. Ruta interna de la automatizacion; no expone el expediente.
     */
    default VisitSummary visitSummary(UUID clinicId, UUID patientId) {
        throw new UnsupportedOperationException();
    }

    /** {@code next*} y {@code lastAttended*} son null si no hay. */
    record VisitSummary(LocalDateTime nextStart, UUID nextDoctorStaffId, LocalDateTime lastAttendedStart,
                        UUID lastAttendedDoctorStaffId, int attended, int cancelled, int noShows) {}

    /**
     * El paciente confirma su propia cita vigente y futura desde el recordatorio de WhatsApp (ruta interna).
     * Si ya estaba confirmada no cambia nada.
     *
     * @throws AppointmentNotConfirmableException si la cita no es suya, ya paso o ya no esta vigente.
     */
    default void confirmByPatient(UUID clinicId, UUID appointmentId, UUID patientId) {
        throw new UnsupportedOperationException();
    }

    class AppointmentNotConfirmableException extends RuntimeException {
        public AppointmentNotConfirmableException(String message) {
            super(message);
        }
    }

    class AppointmentNotCancellableException extends RuntimeException {
        public AppointmentNotCancellableException(String message) {
            super(message);
        }
    }

    record UpcomingAppointment(UUID appointmentId, UUID doctorStaffId, LocalDateTime start, LocalDateTime end,
                               boolean confirmed) {}

    record HoldSlotCommand(UUID clinicId, UUID doctorStaffId, LocalDateTime start, LocalDateTime end,
                           UUID reference, int holdMinutes) {}

    /** {@code serviceId}: servicio del catalogo de la cita (opcional). */
    record BookHeldSlotCommand(UUID clinicId, UUID holdId, UUID patientId, String reason, UUID serviceId) {
        public BookHeldSlotCommand(UUID clinicId, UUID holdId, UUID patientId, String reason) {
            this(clinicId, holdId, patientId, reason, null);
        }
    }

    class SlotUnavailableException extends RuntimeException {
        public SlotUnavailableException(String message) {
            super(message);
        }
    }
}
