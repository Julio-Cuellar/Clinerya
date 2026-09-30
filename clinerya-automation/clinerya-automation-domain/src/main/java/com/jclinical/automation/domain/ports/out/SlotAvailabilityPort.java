package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.AvailableSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SlotAvailabilityPort {
    /** Cupos libres del medico desde {@code from}, durante {@code days} dias, como maximo {@code limit}. */
    List<AvailableSlot> availableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit);

    /** Cupos donde cabe un servicio de {@code durationMinutes} (sin duracion: el largo de la clinica). */
    default List<AvailableSlot> availableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit,
                                               Integer durationMinutes) {
        return availableSlots(clinicId, doctorStaffId, from, days, limit);
    }
}
