package com.jclinical.inventory.domain.ports.in;

import com.jclinical.inventory.domain.model.MaterialReservationDetail;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageMaterialReservationUseCase {

    /**
     * Se dispara al agendar una cita con tratamiento ligado, desde un listener de eventos:
     * no hay usuario en la peticion. No exponer desde un controlador.
     */
    void reserveForAppointment(UUID clinicId, UUID appointmentId, List<ReservationLineCommand> lines);

    /**
     * Se dispara al cancelar/completar una cita, desde un listener de eventos: no hay
     * usuario en la peticion. No exponer desde un controlador.
     */
    void releaseForAppointment(UUID clinicId, UUID appointmentId);

    List<MaterialReservationDetail> listActiveReservations(UUID clinicId, UUID actingUserId);

    record ReservationLineCommand(UUID materialId, String materialName, BigDecimal quantity) {}
}
