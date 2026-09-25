package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Los cupos los calcula la agenda con sus reglas; la automatizacion solo los ofrece. */
public class SlotAvailabilityAdapter implements SlotAvailabilityPort {

    private final OnlineBookingUseCase onlineBooking;

    public SlotAvailabilityAdapter(OnlineBookingUseCase onlineBooking) {
        this.onlineBooking = onlineBooking;
    }

    @Override
    public List<AvailableSlot> availableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit) {
        return onlineBooking.findAvailableSlots(clinicId, doctorStaffId, from, days, limit).stream()
                .map(slot -> new AvailableSlot(slot.start(), slot.end()))
                .toList();
    }
}
