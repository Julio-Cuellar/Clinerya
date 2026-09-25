package com.jclinical.agenda.domain.ports.in;

import java.util.List;
import java.util.UUID;

/** Reglas de la reserva en linea que configura la clinica. */
public interface ManageOnlineBookingSettingsUseCase {

    OnlineBookingSettings getSettings(UUID actingUserId, UUID clinicId);

    void updateSlotMinutes(UUID actingUserId, UUID clinicId, int minutes);

    void updateDoctorLeadMinutes(UUID actingUserId, UUID clinicId, UUID doctorStaffId, int minutes);

    record OnlineBookingSettings(int slotMinutes, List<DoctorLeadTime> doctors) {}

    record DoctorLeadTime(UUID doctorStaffId, String doctorName, int minLeadMinutes) {}
}
