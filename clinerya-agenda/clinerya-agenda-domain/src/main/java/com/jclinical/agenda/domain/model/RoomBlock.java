package com.jclinical.agenda.domain.model;

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
public class RoomBlock {
    private UUID id;
    private UUID clinicId;
    private UUID roomId;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private String reason;
    private RoomBlockType type;
    private UUID createdByUserId;
    @Builder.Default
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean overlapsWith(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return active && startsAt.isBefore(otherEnd) && otherStart.isBefore(endsAt);
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }
}
