package com.jclinical.inventory.domain.ports.in;

import com.jclinical.inventory.domain.model.MaterialReservationDetail;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageMaterialReservationUseCase {

    /** Sin control de permiso: reaccion a eventos de agenda, sin usuario en contexto. */
    void reserveForAppointment(UUID clinicId, UUID appointmentId, List<ReservationLineCommand> lines);

    /** Sin control de permiso: reaccion a eventos de agenda, sin usuario en contexto. */
    void releaseForAppointment(UUID clinicId, UUID appointmentId);

    List<MaterialReservationDetail> listActiveReservations(UUID actingUserId, UUID clinicId);

    record ReservationLineCommand(UUID materialId, String materialName, BigDecimal quantity) {}
}
