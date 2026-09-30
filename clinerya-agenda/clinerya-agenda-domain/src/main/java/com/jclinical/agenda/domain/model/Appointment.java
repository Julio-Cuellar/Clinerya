package com.jclinical.agenda.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    private UUID id;
    private UUID clinicId;
    private UUID patientId;
    private UUID doctorStaffId;
    private UUID roomId;
    private UUID quotationId;
    private UUID quotationItemId;
    @Builder.Default
    private List<UUID> quotationItemIds = List.of();
    /** Servicio del catalogo (opcional). Nombre, tipo y precio son copias del momento de agendar. */
    private UUID serviceId;
    private String serviceName;
    private ServicePricing servicePricing;
    /** Solo si el precio es fijo; con precio variable queda por definir (null). */
    private BigDecimal servicePrice;
    private UUID seriesId;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private String reason;
    private String notes;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private UUID cancelledByUserId;
    private AppointmentStatus status;
    @Builder.Default
    private boolean materialsReserved = false;
    private String externalCalendarEventId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public List<UUID> getQuotationItemIds() {
        if (quotationItemIds != null && !quotationItemIds.isEmpty()) {
            return List.copyOf(quotationItemIds);
        }
        return quotationItemId == null ? List.of() : List.of(quotationItemId);
    }

    public void cancel(String reason, UUID cancelledByUserId) {
        transitionTo(AppointmentStatus.CANCELLED);
        this.cancellationReason = reason;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledByUserId = cancelledByUserId;
    }

    public boolean overlapsWith(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return scheduledStart.isBefore(otherEnd) && otherStart.isBefore(scheduledEnd);
    }

    public boolean isActive() {
        return status != AppointmentStatus.CANCELLED && status != AppointmentStatus.NO_SHOW;
    }

    public void transitionTo(AppointmentStatus target) {
        if (status == AppointmentStatus.COMPLETED || status == AppointmentStatus.CANCELLED || status == AppointmentStatus.NO_SHOW) {
            throw new IllegalStateException("No se puede cambiar el estado de una cita en estado terminal (" + status + ").");
        }
        this.status = target;
        this.updatedAt = LocalDateTime.now();
    }

    public void reschedule(LocalDateTime newStart, LocalDateTime newEnd) {
        if (status != AppointmentStatus.SCHEDULED && status != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Solo se pueden reagendar citas programadas o confirmadas.");
        }
        this.scheduledStart = newStart;
        this.scheduledEnd = newEnd;
        this.updatedAt = LocalDateTime.now();
    }
}
