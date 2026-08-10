package com.jclinical.integrations.domain.model;

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
public class ExternalCalendarEvent {
    private UUID id;
    private UUID clinicId;
    private UUID staffId;
    private String googleEventId;
    private String summary;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ExternalEventStatus status;
    private UUID linkedAppointmentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
