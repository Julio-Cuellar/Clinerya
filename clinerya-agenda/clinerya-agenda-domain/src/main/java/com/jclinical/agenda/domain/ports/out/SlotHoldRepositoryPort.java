package com.jclinical.agenda.domain.ports.out;

import com.jclinical.agenda.domain.model.SlotHold;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotHoldRepositoryPort {

    SlotHold save(SlotHold hold);

    Optional<SlotHold> findByIdAndClinicId(UUID holdId, UUID clinicId);

    /** Apartados vigentes a {@code now} del medico que se traslapan con [from, to). */
    List<SlotHold> findActiveByDoctorAndRange(UUID doctorStaffId, UUID clinicId, LocalDateTime from,
                                              LocalDateTime to, LocalDateTime now);

    /**
     * Serializa, dentro de la transaccion en curso, todo apartado o cita del medico: dos pacientes no
     * pueden apartar el mismo horario aunque confirmen en el mismo instante.
     */
    void lockDoctorSchedule(UUID clinicId, UUID doctorStaffId);
}
