package com.jclinical.agenda.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface OnlineBookingSettingsRepositoryPort {

    Optional<Integer> findSlotMinutes(UUID clinicId);

    void saveSlotMinutes(UUID clinicId, int minutes);

    Optional<Integer> findLeadMinutes(UUID clinicId, UUID doctorStaffId);

    void saveLeadMinutes(UUID clinicId, UUID doctorStaffId, int minutes);
}
