package com.jclinical.records.domain.ports.out;

import java.util.UUID;

public interface PatientAccessAuthorizationPort {

    AccessDecision resolveAccess(UUID requestingUserId, UUID clinicId, UUID patientId);

    enum AccessLevel { NONE, READ_ONLY, COMMENT, READ_WRITE }

    record AccessDecision(AccessLevel level, boolean viaExternalGrant) {}
}
