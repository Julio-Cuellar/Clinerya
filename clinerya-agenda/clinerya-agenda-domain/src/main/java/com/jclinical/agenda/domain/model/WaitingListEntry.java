package com.jclinical.agenda.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitingListEntry {
    private UUID id;
    private UUID clinicId;
    private UUID patientId;
    private UUID doctorStaffId;
    private UUID roomId;
    private LocalDate preferredDateFrom;
    private LocalDate preferredDateTo;
    private String preferredTimeRange; // "MORNING", "AFTERNOON", "ANY"
    private String notes;
    @Builder.Default
    private Status status = Status.WAITING;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Status {
        WAITING,
        NOTIFIED,
        SCHEDULED,
        CANCELLED
    }

    public void markNotified() {
        this.status = Status.NOTIFIED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markScheduled() {
        this.status = Status.SCHEDULED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCancelled() {
        this.status = Status.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }
}
