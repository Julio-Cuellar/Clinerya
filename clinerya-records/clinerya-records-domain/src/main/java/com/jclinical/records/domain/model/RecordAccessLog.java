package com.jclinical.records.domain.model;

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
public class RecordAccessLog {
    private UUID id;
    private UUID clinicId;
    private UUID patientId;
    private UUID userId;
    private String userName;
    private String resourceType;
    private UUID resourceId;
    private String actionType;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
