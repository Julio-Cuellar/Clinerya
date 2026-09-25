package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.model.AppointmentRequest;
import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.ports.in.ExpireAppointmentRequestsUseCase;
import com.jclinical.automation.domain.ports.in.RespondAppointmentRequestUseCase;
import com.jclinical.automation.domain.service.AppointmentRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Cada respuesta del medico toca la agenda (bloqueo por medico, apartados, cita), la solicitud y el
 * outbox: todo en una transaccion. La conversacion usa el servicio sin envolver, dentro de la suya.
 */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalAppointmentRequestUseCase implements RespondAppointmentRequestUseCase, ExpireAppointmentRequestsUseCase {

    private final AppointmentRequestService requests;

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentRequest> listPending(UUID actingUserId, UUID clinicId) {
        return requests.listPending(actingUserId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlot> proposableSlots(UUID actingUserId, UUID clinicId, UUID requestId) {
        return requests.proposableSlots(actingUserId, clinicId, requestId);
    }

    @Override
    @Transactional
    public AppointmentRequest accept(UUID actingUserId, UUID clinicId, UUID requestId) {
        return requests.accept(actingUserId, clinicId, requestId);
    }

    @Override
    @Transactional
    public AppointmentRequest reject(UUID actingUserId, UUID clinicId, UUID requestId, String reason) {
        return requests.reject(actingUserId, clinicId, requestId, reason);
    }

    @Override
    @Transactional
    public AppointmentRequest proposeOptions(UUID actingUserId, UUID clinicId, UUID requestId, List<AvailableSlot> options) {
        return requests.proposeOptions(actingUserId, clinicId, requestId, options);
    }

    @Override
    @Transactional
    public int expireOverdue() {
        return requests.expireOverdue();
    }
}
