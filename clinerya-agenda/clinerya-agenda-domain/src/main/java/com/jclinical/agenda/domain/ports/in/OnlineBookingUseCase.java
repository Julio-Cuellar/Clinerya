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

    record HoldSlotCommand(UUID clinicId, UUID doctorStaffId, LocalDateTime start, LocalDateTime end,
                           UUID reference, int holdMinutes) {}

    record BookHeldSlotCommand(UUID clinicId, UUID holdId, UUID patientId, String reason) {}

    class SlotUnavailableException extends RuntimeException {
        public SlotUnavailableException(String message) {
            super(message);
        }
    }
}
