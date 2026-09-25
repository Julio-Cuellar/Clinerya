package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.service.OnlineBookingService;
import com.jclinical.automation.domain.ports.out.SlotBookingPort;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class SlotBookingAdapter implements SlotBookingPort {

    public SlotBookingAdapter(OnlineBookingService onlineBooking) {
    }

    @Override
    public UUID hold(UUID clinicId, UUID doctorStaffId, LocalDateTime start, LocalDateTime end, UUID reference, int holdMinutes) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public void release(UUID clinicId, UUID holdId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public UUID book(UUID clinicId, UUID holdId, UUID patientId, String reason) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public Optional<UUID> doctorStaffIdOfUser(UUID clinicId, UUID userId) {
        throw new UnsupportedOperationException("pendiente");
    }
}
