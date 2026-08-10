package com.jclinical.agenda.domain.ports.in;

public interface ManageMaterialReservationSchedulingUseCase {

    /**
     * Recorre las citas ligadas a partidas de cotización que aún no han disparado su reserva
     * de material, y para las que ya entraron en la ventana de "X días antes de la cita"
     * (configurable por clínica), publica el evento de reserva y marca la cita como procesada.
     *
     * @return número de citas procesadas en esta ejecución.
     */
    int processPendingReservations();
}
