package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.in.ManageMaterialReservationSchedulingUseCase;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.ClinicSettingsPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort.AcceptedQuotationSnapshot;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort.QuotationItemSnapshot;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import com.jclinical.core.events.MaterialReservationRequestedEvent;
import com.jclinical.core.events.MaterialReservationRequestedEvent.ReservationLine;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MaterialReservationSchedulingService implements ManageMaterialReservationSchedulingUseCase {

    private final AppointmentRepositoryPort appointmentRepository;
    private final QuotationValidatorPort quotationValidator;
    private final ClinicSettingsPort clinicSettingsPort;
    private final DomainEventPublisherPort eventPublisher;

    public MaterialReservationSchedulingService(
            AppointmentRepositoryPort appointmentRepository,
            QuotationValidatorPort quotationValidator,
            ClinicSettingsPort clinicSettingsPort,
            DomainEventPublisherPort eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.quotationValidator = quotationValidator;
        this.clinicSettingsPort = clinicSettingsPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public int processPendingReservations() {
        List<Appointment> candidates = appointmentRepository.findPendingMaterialReservationCandidates();
        LocalDateTime now = LocalDateTime.now();
        int processed = 0;

        for (Appointment appointment : candidates) {
            if (processReservation(appointment, now)) {
                processed++;
            }
        }

        return processed;
    }

    /**
     * Intenta reservar inmediatamente una cita recién creada o reagendada. Si todavía
     * está fuera de la ventana configurada, permanece pendiente para el scheduler.
     */
    public boolean processReservation(Appointment appointment) {
        return processReservation(appointment, LocalDateTime.now());
    }

    private boolean processReservation(Appointment appointment, LocalDateTime now) {
        if (!isReservationCandidate(appointment)) {
            return false;
        }

        int leadDays = clinicSettingsPort.getMaterialReservationLeadDays(appointment.getClinicId());
        if (appointment.getScheduledStart().isAfter(now.plusDays(leadDays))) {
            return false;
        }

        AcceptedQuotationSnapshot snapshot = quotationValidator.findAcceptedQuotation(
                        appointment.getQuotationId(), appointment.getPatientId(), appointment.getClinicId())
                .orElse(null);
        if (snapshot == null) {
            return false;
        }

        boolean processed = false;
        for (UUID quotationItemId : appointment.getQuotationItemIds()) {
            QuotationItemSnapshot selectedItem = snapshot.items().stream()
                    .filter(candidate -> candidate.itemId().equals(quotationItemId))
                    .findFirst()
                    .orElse(null);
            if (selectedItem == null) {
                return false;
            }

            List<ReservationLine> lines = selectedItem.materials() == null
                    ? List.of()
                    : selectedItem.materials().stream()
                            .filter(material -> material.materialId() != null
                                    && material.estimatedQuantity() != null
                                    && material.estimatedQuantity().signum() > 0)
                            .map(material -> new ReservationLine(
                                    material.materialId(), material.materialName(), material.estimatedQuantity()))
                            .toList();
            if (!lines.isEmpty()) {
                eventPublisher.publish(DomainEventRoutingKeys.MATERIAL_RESERVATION_REQUESTED, new MaterialReservationRequestedEvent(
                        UUID.randomUUID(),
                        appointment.getClinicId(),
                        appointment.getId(),
                        appointment.getQuotationId(),
                        quotationItemId,
                        now,
                        lines
                ));
            }
            processed = true;
        }

        if (!processed) {
            return false;
        }

        markReservationProcessed(appointment);
        return true;
    }

    private boolean isReservationCandidate(Appointment appointment) {
        return appointment != null
                && !appointment.isMaterialsReserved()
                && appointment.getQuotationId() != null
                && !appointment.getQuotationItemIds().isEmpty()
                && appointment.getScheduledStart() != null
                && (appointment.getStatus() == AppointmentStatus.SCHEDULED
                || appointment.getStatus() == AppointmentStatus.CONFIRMED);
    }

    private void markReservationProcessed(Appointment appointment) {
        appointment.setMaterialsReserved(true);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }
}
