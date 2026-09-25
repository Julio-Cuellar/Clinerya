package com.jclinical.agenda.domain.ports.in;

import com.jclinical.agenda.domain.model.BookableSlot;
import com.jclinical.agenda.domain.model.SlotHold;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    record HoldSlotCommand(UUID clinicId, UUID doctorStaffId, LocalDateTime start, LocalDateTime end,
                           UUID reference, int holdMinutes) {}

    class SlotUnavailableException extends RuntimeException {
        public SlotUnavailableException(String message) {
            super(message);
        }
    }
}
