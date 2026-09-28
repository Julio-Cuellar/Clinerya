package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.automation.domain.ports.out.AppointmentCancellationPort;

import java.util.UUID;

/** Si la cita es del paciente y aun se puede cancelar lo decide la agenda; aqui solo se traduce. */
public class AppointmentCancellationAdapter implements AppointmentCancellationPort {

    private final OnlineBookingUseCase agenda;

    public AppointmentCancellationAdapter(OnlineBookingUseCase agenda) {
        this.agenda = agenda;
    }

    @Override
    public void cancel(UUID clinicId, UUID appointmentId, UUID patientId, String reason) {
        try {
            agenda.cancelByPatient(clinicId, appointmentId, patientId, reason);
        } catch (OnlineBookingUseCase.AppointmentNotCancellableException refused) {
            throw new NotCancellableException(refused.getMessage());
        }
    }
}
