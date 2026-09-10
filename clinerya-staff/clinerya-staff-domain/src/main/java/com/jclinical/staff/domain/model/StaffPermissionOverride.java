package com.jclinical.staff.domain.model;

import com.jclinical.core.security.StaffPermission;
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
public class StaffPermissionOverride {
    private UUID id;
    private UUID clinicId;
    private UUID staffId;
    private StaffPermission permission;
    private StaffPermissionOverrideState state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
