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
public class MedicalHistoryVersion {
    private UUID id;
    private UUID medicalHistoryId;
    private int version;
    private String answersJson;
    private UUID changedByUserId;
    private String changedByUserName;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
