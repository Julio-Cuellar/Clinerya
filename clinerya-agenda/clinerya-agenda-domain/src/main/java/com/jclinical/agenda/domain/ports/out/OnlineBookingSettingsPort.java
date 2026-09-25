package com.jclinical.agenda.domain.ports.out;

import java.util.UUID;

/** Reglas de la reserva en linea. Sin configurar, aplican los valores por defecto. */
public interface OnlineBookingSettingsPort {

    int DEFAULT_SLOT_MINUTES = 30;
    int DEFAULT_MIN_LEAD_MINUTES = 120;

    /** Duracion de cada cupo ofrecido; la define la clinica. */
    int slotMinutes(UUID clinicId);

    /** Anticipacion minima con la que se puede pedir un cupo; la define cada medico. */
    int minLeadMinutes(UUID clinicId, UUID doctorStaffId);
}
