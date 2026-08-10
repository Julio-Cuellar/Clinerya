package com.jclinical.staff.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffActivityLog {
    private UUID id;
    private UUID clinicId;
    private UUID staffId;
    private StaffActivityType type;
    private String referenceType;
    private UUID referenceId;
    private String description;
    private BigDecimal amount;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}
