package com.jclinical.cash.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface CashStaffValidatorPort {

    Optional<StaffSnapshot> findActiveStaff(UUID staffId, UUID clinicId);

    record StaffSnapshot(UUID staffId, String fullName) {}
}
