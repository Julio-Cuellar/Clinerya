package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AppointmentRequest;
import com.jclinical.automation.domain.model.AppointmentRequest.ProposedOption;
import com.jclinical.automation.domain.model.AppointmentRequest.Status;
import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent;
import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent.Outcome;
import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.ports.in.ExpireAppointmentRequestsUseCase;
import com.jclinical.automation.domain.ports.in.RespondAppointmentRequestUseCase;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort;
import com.jclinical.automation.domain.ports.out.AppointmentRequestRepositoryPort;
import com.jclinical.automation.domain.ports.out.DoctorAlertPort;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;
import com.jclinical.automation.domain.ports.out.SlotBookingPort;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import com.jclinical.core.security.ClinicAccessDeniedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Ciclo de vida de una solicitud de cita (CU-3). El paciente la envia desde la conversacion; solo el
 * medico asignado la acepta, la rechaza o propone otros horarios. Cada respuesta del medico (y el
 * vencimiento) se publica como {@link AppointmentRequestResolvedEvent} para que la conversacion del
 * paciente reaccione; la agenda solo se toca por {@link SlotBookingPort}.
 */
public class AppointmentRequestService
        implements AppointmentRequestPort, RespondAppointmentRequestUseCase, ExpireAppointmentRequestsUseCase {

    static final int HOLD_MINUTES = (int) AppointmentRequest.RESPONSE_WINDOW.toMinutes();
    static final int MAX_PROPOSED_OPTIONS = 3;
    static final int PROPOSABLE_SEARCH_DAYS = 14;
    static final int MAX_PROPOSABLE_SLOTS = 30;
    static final int MAX_REASON_LENGTH = 500;
    static final String BOOKING_REASON = "Solicitada por WhatsApp";

    private final AppointmentRequestRepositoryPort requests;
    private final SlotBookingPort booking;
    private final SlotAvailabilityPort slots;
    private final DomainEventPublisherPort events;
    private final DoctorAlertPort alerts;
    private final Clock clock;

    public AppointmentRequestService(AppointmentRequestRepositoryPort requests, SlotBookingPort booking,
                                     SlotAvailabilityPort slots, DomainEventPublisherPort events, DoctorAlertPort alerts,
                                     Clock clock) {
        this.requests = requests;
        this.booking = booking;
        this.slots = slots;
        this.events = events;
        this.alerts = alerts;
        this.clock = clock;
    }

    // ---- paciente ---------------------------------------------------------------------------

    @Override
    public UUID submit(NewAppointmentRequest request) {
        UUID requestId = UUID.randomUUID();
        UUID holdId = booking.hold(request.clinicId(), request.doctorStaffId(), request.start(), request.end(),
                requestId, HOLD_MINUTES);
        AppointmentRequest saved = requests.save(new AppointmentRequest(requestId, request.clinicId(),
                request.conversationId(), request.patientId(), request.patientName(), request.patientPhone(),
                request.doctorStaffId(), request.doctorName(), request.start(), request.end(), holdId, Status.PENDING,
                List.of(), null, null, now(), null));
        alerts.newRequest(saved);
        return requestId;
    }

    @Override
    public UUID chooseOption(UUID clinicId, UUID requestId, LocalDateTime start, LocalDateTime end) {
        AppointmentRequest request = find(clinicId, requestId);
        if (request.status() != Status.OPTIONS_PROPOSED || request.isOverdueAt(now())) {
            throw new SlotNoLongerAvailableException("La propuesta del médico ya no está vigente.");
        }
        ProposedOption chosen = request.proposedOptions().stream()
                .filter(option -> option.start().equals(start) && option.end().equals(end))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ese horario no está entre las opciones propuestas."));

        UUID appointmentId = booking.book(clinicId, chosen.holdId(), request.patientId(), BOOKING_REASON);
        request.proposedOptions().stream()
                .filter(option -> !option.holdId().equals(chosen.holdId()))
                .forEach(option -> booking.release(clinicId, option.holdId()));
        requests.save(request.booked(appointmentId, now()));
        return appointmentId;
    }

    @Override
    public void declineOptions(UUID clinicId, UUID requestId) {
        AppointmentRequest request = find(clinicId, requestId);
        if (request.status() != Status.OPTIONS_PROPOSED) {
            return;
        }
        request.proposedOptions().forEach(option -> booking.release(clinicId, option.holdId()));
        requests.save(request.declined());
    }

    // ---- medico -----------------------------------------------------------------------------

    @Override
    public List<AppointmentRequest> listPending(UUID actingUserId, UUID clinicId) {
        if (actingUserId == null) {
            return List.of();
        }
        LocalDateTime now = now();
        return booking.doctorStaffIdOfUser(clinicId, actingUserId)
                .map(doctorStaffId -> requests.findPendingByDoctor(clinicId, doctorStaffId).stream()
                        .filter(request -> !request.isOverdueAt(now))
                        .toList())
                .orElse(List.of());
    }

    @Override
    public List<AvailableSlot> proposableSlots(UUID actingUserId, UUID clinicId, UUID requestId) {
        AppointmentRequest request = pendingOfDoctor(actingUserId, clinicId, requestId);
        return slots.availableSlots(clinicId, request.doctorStaffId(), now().toLocalDate(),
                PROPOSABLE_SEARCH_DAYS, MAX_PROPOSABLE_SLOTS);
    }

    @Override
    public AppointmentRequest accept(UUID actingUserId, UUID clinicId, UUID requestId) {
        AppointmentRequest request = pendingOfDoctor(actingUserId, clinicId, requestId);
        UUID appointmentId;
        try {
            appointmentId = booking.book(clinicId, request.holdId(), request.patientId(), BOOKING_REASON);
        } catch (SlotNoLongerAvailableException refused) {
            throw new IllegalStateException(refused.getMessage());
        }
        AppointmentRequest booked = requests.save(request.booked(appointmentId, now()));
        publish(booked, Outcome.BOOKED, List.of());
        return booked;
    }

    @Override
    public AppointmentRequest reject(UUID actingUserId, UUID clinicId, UUID requestId, String reason) {
        String cleanReason = reason == null || reason.isBlank() ? null : reason.trim();
        if (cleanReason != null && cleanReason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("El motivo no puede pasar de " + MAX_REASON_LENGTH + " caracteres.");
        }
        AppointmentRequest request = pendingOfDoctor(actingUserId, clinicId, requestId);
        booking.release(clinicId, request.holdId());
        AppointmentRequest rejected = requests.save(request.rejected(cleanReason, now()));
        publish(rejected, Outcome.REJECTED, List.of());
        return rejected;
    }

    /**
     * Las opciones se apartan antes de soltar el cupo original: si alguna ya no esta libre, se
     * liberan las que se alcanzaron a apartar y la solicitud queda como estaba.
     */
    @Override
    public AppointmentRequest proposeOptions(UUID actingUserId, UUID clinicId, UUID requestId, List<AvailableSlot> options) {
        AppointmentRequest request = pendingOfDoctor(actingUserId, clinicId, requestId);
        validateProposal(request, options);

        List<ProposedOption> held = new ArrayList<>();
        for (AvailableSlot option : options) {
            try {
                UUID holdId = booking.hold(clinicId, request.doctorStaffId(), option.start(), option.end(),
                        request.id(), HOLD_MINUTES);
                held.add(new ProposedOption(holdId, option.start(), option.end()));
            } catch (SlotNoLongerAvailableException taken) {
                held.forEach(alreadyHeld -> booking.release(clinicId, alreadyHeld.holdId()));
                throw new IllegalStateException("Uno de los horarios propuestos ya no está disponible. Elige otros.");
            }
        }
        booking.release(clinicId, request.holdId());
        AppointmentRequest proposed = requests.save(request.optionsProposed(held, now()));
        publish(proposed, Outcome.OPTIONS_PROPOSED, List.copyOf(options));
        return proposed;
    }

    // ---- vencimiento ------------------------------------------------------------------------

    @Override
    public int expireOverdue() {
        LocalDateTime now = now();
        int expired = 0;
        for (AppointmentRequest request : requests.findOverdue(now.minus(AppointmentRequest.RESPONSE_WINDOW))) {
            if (!request.isOverdueAt(now)) {
                continue;
            }
            releaseHoldsOf(request);
            publish(requests.save(request.expired()), Outcome.EXPIRED, List.of());
            expired++;
        }
        return expired;
    }

    // ---- utilidades -------------------------------------------------------------------------

    private AppointmentRequest pendingOfDoctor(UUID actingUserId, UUID clinicId, UUID requestId) {
        AppointmentRequest request = find(clinicId, requestId);
        UUID actingDoctor = actingUserId == null ? null : booking.doctorStaffIdOfUser(clinicId, actingUserId).orElse(null);
        if (!request.doctorStaffId().equals(actingDoctor)) {
            throw new ClinicAccessDeniedException("Solo el médico de la cita puede responder esta solicitud.");
        }
        if (request.status() != Status.PENDING) {
            throw new IllegalStateException("Esta solicitud ya fue respondida.");
        }
        if (request.isOverdueAt(now())) {
            throw new IllegalStateException("La solicitud venció: pasaron 24 horas sin respuesta.");
        }
        return request;
    }

    private AppointmentRequest find(UUID clinicId, UUID requestId) {
        return requests.findByIdAndClinicId(requestId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La solicitud no existe."));
    }

    private static void validateProposal(AppointmentRequest request, List<AvailableSlot> options) {
        if (options == null || options.isEmpty() || options.size() > MAX_PROPOSED_OPTIONS) {
            throw new IllegalArgumentException("Propón entre 1 y " + MAX_PROPOSED_OPTIONS + " horarios.");
        }
        if (new HashSet<>(options).size() != options.size()) {
            throw new IllegalArgumentException("No repitas horarios en la propuesta.");
        }
        boolean includesRequested = options.stream()
                .anyMatch(option -> option.start().equals(request.start()) && option.end().equals(request.end()));
        if (includesRequested) {
            throw new IllegalArgumentException("Ese es el horario que pidió el paciente: acéptalo en lugar de proponerlo.");
        }
    }

    private void releaseHoldsOf(AppointmentRequest request) {
        if (request.status() == Status.PENDING) {
            booking.release(request.clinicId(), request.holdId());
        } else {
            request.proposedOptions().forEach(option -> booking.release(request.clinicId(), option.holdId()));
        }
    }

    private void publish(AppointmentRequest request, Outcome outcome, List<AvailableSlot> options) {
        events.publish(DomainEventRoutingKeys.APPOINTMENT_REQUEST_RESOLVED, new AppointmentRequestResolvedEvent(
                UUID.randomUUID(), request.clinicId(), request.id(), request.conversationId(), outcome,
                request.doctorName(), request.start(), request.end(), request.rejectionReason(), options, now()));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
