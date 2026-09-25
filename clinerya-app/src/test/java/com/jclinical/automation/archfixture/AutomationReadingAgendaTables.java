package com.jclinical.automation.archfixture;

import com.jclinical.agenda.infra.adapters.out.persistence.AppointmentEntity;
import com.jclinical.agenda.infra.adapters.out.persistence.SpringDataAppointmentRepository;

/**
 * Ejemplo de lo que la regla de acceso a datos prohibe: la automatizacion leyendo las tablas de la
 * agenda por su persistencia. Solo existe para comprobar que la regla lo detecta; no es codigo real.
 */
public class AutomationReadingAgendaTables {

    AppointmentEntity appointmentRow;

    SpringDataAppointmentRepository appointmentTable;
}
