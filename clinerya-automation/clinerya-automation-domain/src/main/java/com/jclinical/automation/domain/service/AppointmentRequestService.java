package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AppointmentRequest;
import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.ports.in.ExpireAppointmentRequestsUseCase;
import com.jclinical.automation.domain.ports.in.RespondAppointmentRequestUseCase;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort;
import com.jclinical.automation.domain.ports.out.AppointmentRequestRepositoryPort;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;
import com.jclinical.automation.domain.ports.out.SlotBookingPort;
import com.jclinical.core.events.DomainEventPublisherPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AppointmentRequestService
        implements AppointmentRequestPort, RespondAppointmentRequestUseCase, ExpireAppointmentRequestsUseCase {

    public AppointmentRequestService(AppointmentRequestRepositoryPort requests, SlotBookingPort booking,
                                     SlotAvailabilityPort slots, DomainEventPublisherPort events, Clock clock) {
    }

    @Override
    public UUID submit(NewAppointmentRequest request) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public UUID chooseOption(UUID clinicId, UUID requestId, LocalDateTime start, LocalDateTime end) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public void declineOptions(UUID clinicId, UUID requestId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public List<AppointmentRequest> listPending(UUID actingUserId, UUID clinicId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public List<AvailableSlot> proposableSlots(UUID actingUserId, UUID clinicId, UUID requestId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public AppointmentRequest accept(UUID actingUserId, UUID clinicId, UUID requestId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public AppointmentRequest reject(UUID actingUserId, UUID clinicId, UUID requestId, String reason) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public AppointmentRequest proposeOptions(UUID actingUserId, UUID clinicId, UUID requestId, List<AvailableSlot> options) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public int expireOverdue() {
        throw new UnsupportedOperationException("pendiente");
    }
}
