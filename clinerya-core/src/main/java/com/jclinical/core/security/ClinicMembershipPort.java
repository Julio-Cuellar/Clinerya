package com.jclinical.core.security;

import java.util.UUID;

public interface ClinicMembershipPort {

    boolean isActiveStaffMember(UUID userId, UUID clinicId);
}
