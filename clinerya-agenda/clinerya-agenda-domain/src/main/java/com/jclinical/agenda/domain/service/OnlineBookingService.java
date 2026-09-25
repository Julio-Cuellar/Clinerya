package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.BookableSlot;
import com.jclinical.agenda.domain.model.SlotHold;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsPort;
import com.jclinical.agenda.domain.ports.out.SlotHoldRepositoryPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class OnlineBookingService implements OnlineBookingUseCase {

    public OnlineBookingService(ClinicScheduleService clinicScheduleService, AppointmentRepositoryPort appointments,
                                SlotHoldRepositoryPort holds, OnlineBookingSettingsPort settings,
                                StaffValidatorPort staffValidator, Clock clock) {
    }

    @Override
    public List<BookableSlot> findAvailableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public SlotHold holdSlot(HoldSlotCommand command) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public void releaseHold(UUID clinicId, UUID holdId) {
        throw new UnsupportedOperationException("pendiente");
    }
}
