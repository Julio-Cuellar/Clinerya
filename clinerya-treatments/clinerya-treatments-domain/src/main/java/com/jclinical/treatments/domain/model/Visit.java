package com.jclinical.treatments.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Visit {
    private final UUID id;
    private final UUID clinicId;
    private final UUID patientId;
    private final UUID quotationId;
    private final LocalDate visitDate;
    private final UUID doctorId;
    private final String notes;
    private final List<VisitLineItem> items;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Visit(
            UUID id,
            UUID clinicId,
            UUID patientId,
            UUID quotationId,
            LocalDate visitDate,
            UUID doctorId,
            String notes,
            List<VisitLineItem> items,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.clinicId = clinicId;
        this.patientId = patientId;
        this.quotationId = quotationId;
        this.visitDate = visitDate;
        this.doctorId = doctorId;
        this.notes = notes;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClinicId() {
        return clinicId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getQuotationId() {
        return quotationId;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public String getNotes() {
        return notes;
    }

    public List<VisitLineItem> getItems() {
        return items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
