package com.jclinical.clinics.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicRoomStaffAssignment {
    private UUID id;
    private UUID clinicId;
    private UUID roomId;
    private UUID staffId;
    @Builder.Default
    private boolean active = true;
    private LocalDateTime assignedAt;
    private LocalDateTime unassignedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void reactivate() {
        this.active = true;
        this.unassignedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.unassignedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
