package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.AppointmentReminder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Recordatorios de cita, uno por cita. */
public interface AppointmentReminderRepositoryPort {

    Optional<AppointmentReminder> find(UUID appointmentId);

    /** Crea o reemplaza el recordatorio de esa cita. */
    void save(AppointmentReminder reminder);

    void delete(UUID appointmentId);

    /** Los que ya deben salir y no han salido, los mas antiguos primero. */
    List<AppointmentReminder> findDue(LocalDateTime now, int limit);

    /** @return false si otro barrido ya lo habia marcado (entonces no se envia de nuevo) */
    boolean markSent(UUID appointmentId, LocalDateTime at);
}
