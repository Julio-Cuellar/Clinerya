package com.jclinical.agenda.infra.adapters.out.persistence;

import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.model.ServicePricing;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "appointments", schema = "agenda")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEntity {

    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "doctor_staff_id", nullable = false)
    private UUID doctorStaffId;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "quotation_id")
    private UUID quotationId;

    @Column(name = "quotation_item_id")
    private UUID quotationItemId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "appointment_quotation_items",
            schema = "agenda",
            joinColumns = @JoinColumn(name = "appointment_id"))
    @Column(name = "quotation_item_id", nullable = false)
    @Builder.Default
    private List<UUID> quotationItemIds = new ArrayList<>();

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "service_name", length = 200)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_pricing", length = 30)
    private ServicePricing servicePricing;

    @Column(name = "service_price", precision = 12, scale = 2)
    private BigDecimal servicePrice;

    @Column(name = "series_id")
    private UUID seriesId;

    @Column(name = "scheduled_start", nullable = false)
    private LocalDateTime scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private LocalDateTime scheduledEnd;

    @Column
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by_user_id")
    private UUID cancelledByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Column(name = "materials_reserved", nullable = false)
    private boolean materialsReserved;

    @Column(name = "external_calendar_event_id")
    private String externalCalendarEventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
