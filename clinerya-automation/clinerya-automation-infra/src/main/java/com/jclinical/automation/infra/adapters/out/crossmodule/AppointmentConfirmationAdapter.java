package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.AppointmentNotConfirmableException;
import com.jclinical.automation.domain.ports.out.AppointmentConfirmationPort;

import java.util.UUID;

/** El paciente confirma su cita por la ruta publica de la agenda; si ya no se puede, se le dice. */
public class AppointmentConfirmationAdapter implements AppointmentConfirmationPort {

    private final OnlineBookingUseCase agenda;

    public AppointmentConfirmationAdapter(OnlineBookingUseCase agenda) {
        this.agenda = agenda;
    }

    @Override
    public boolean confirm(UUID clinicId, UUID appointmentId, UUID patientId) {
        try {
            agenda.confirmByPatient(clinicId, appointmentId, patientId);
            return true;
        } catch (AppointmentNotConfirmableException refused) {
            return false;
        }
    }
}
