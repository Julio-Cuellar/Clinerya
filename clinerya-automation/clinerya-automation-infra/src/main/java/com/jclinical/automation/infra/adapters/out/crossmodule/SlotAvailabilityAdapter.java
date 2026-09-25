package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class SlotAvailabilityAdapter implements SlotAvailabilityPort {

    public SlotAvailabilityAdapter(OnlineBookingUseCase onlineBooking) {
    }

    @Override
    public List<AvailableSlot> availableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit) {
        return List.of();
    }
}
