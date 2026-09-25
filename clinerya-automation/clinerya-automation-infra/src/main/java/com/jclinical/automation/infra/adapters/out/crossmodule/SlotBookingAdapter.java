package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.BookHeldSlotCommand;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.HoldSlotCommand;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.SlotUnavailableException;
import com.jclinical.agenda.domain.service.OnlineBookingService;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort.SlotNoLongerAvailableException;
import com.jclinical.automation.domain.ports.out.SlotBookingPort;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Apartados y citas en la agenda. Recibe el servicio de dominio de la agenda y no su envoltorio
 * transaccional: siempre corre dentro de una transaccion de la automatizacion (que sostiene el
 * bloqueo por medico), y una negativa de la agenda no debe marcarla para rollback, porque la
 * conversacion la maneja ofreciendo otra cosa.
 */
public class SlotBookingAdapter implements SlotBookingPort {

    private final OnlineBookingService onlineBooking;

    public SlotBookingAdapter(OnlineBookingService onlineBooking) {
        this.onlineBooking = onlineBooking;
    }

    @Override
    public UUID hold(UUID clinicId, UUID doctorStaffId, LocalDateTime start, LocalDateTime end, UUID reference, int holdMinutes) {
        try {
            return onlineBooking.holdSlot(new HoldSlotCommand(clinicId, doctorStaffId, start, end, reference, holdMinutes)).id();
        } catch (SlotUnavailableException unavailable) {
            throw new SlotNoLongerAvailableException(unavailable.getMessage());
        }
    }

    @Override
    public void release(UUID clinicId, UUID holdId) {
        onlineBooking.releaseHold(clinicId, holdId);
    }

    @Override
    public UUID book(UUID clinicId, UUID holdId, UUID patientId, String reason) {
        try {
            return onlineBooking.bookHeldSlot(new BookHeldSlotCommand(clinicId, holdId, patientId, reason));
        } catch (SlotUnavailableException unavailable) {
            throw new SlotNoLongerAvailableException(unavailable.getMessage());
        }
    }

    @Override
    public Optional<UUID> doctorStaffIdOfUser(UUID clinicId, UUID userId) {
        return onlineBooking.doctorStaffIdOfUser(clinicId, userId);
    }
}
