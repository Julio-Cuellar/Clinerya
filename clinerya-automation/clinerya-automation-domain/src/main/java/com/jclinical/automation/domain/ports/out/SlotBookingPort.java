package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.ports.out.AppointmentRequestPort.SlotNoLongerAvailableException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Apartados y citas en la agenda. La automatizacion nunca escribe la agenda por otra via. */
public interface SlotBookingPort {

    /** @throws SlotNoLongerAvailableException si el cupo ya no se puede apartar. */
    UUID hold(UUID clinicId, UUID doctorStaffId, LocalDateTime start, LocalDateTime end, UUID reference, int holdMinutes);

    void release(UUID clinicId, UUID holdId);

    /**
     * Convierte el apartado en cita.
     *
     * @return id de la cita.
     * @throws SlotNoLongerAvailableException con el motivo de la agenda si no se pudo.
     */
    UUID book(UUID clinicId, UUID holdId, UUID patientId, String reason);

    /** Agenda el apartado como cita de un servicio del catalogo (la cita copia su precio). */
    default UUID book(UUID clinicId, UUID holdId, UUID patientId, String reason, UUID serviceId) {
        return book(clinicId, holdId, patientId, reason);
    }

    /** Miembro del personal que corresponde al usuario de Clinerya en la clinica. */
    Optional<UUID> doctorStaffIdOfUser(UUID clinicId, UUID userId);
}
