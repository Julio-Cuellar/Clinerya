package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort;

import java.util.List;
import java.util.UUID;

/** Las proximas citas del paciente las decide la agenda (vigentes y futuras); aqui solo se traducen. */
public class PatientAppointmentsAdapter implements PatientAppointmentsPort {

    private final OnlineBookingUseCase agenda;

    public PatientAppointmentsAdapter(OnlineBookingUseCase agenda) {
        this.agenda = agenda;
    }

    @Override
    public List<UpcomingVisit> upcoming(UUID clinicId, UUID patientId, int limit) {
        return agenda.upcomingAppointments(clinicId, patientId, limit).stream()
                .map(appointment -> new UpcomingVisit(appointment.appointmentId(), appointment.doctorStaffId(),
                        appointment.start(), appointment.end(), appointment.confirmed()))
                .toList();
    }
}
