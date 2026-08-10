package com.jclinical.cash.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.cash.domain.ports.out.CashAppointmentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CashAppointmentAdapter implements CashAppointmentPort {

    private final ManageAppointmentsUseCase appointmentsUseCase;

    @Override
    public List<CompletedAppointmentSnapshot> listCompletedAppointments(UUID clinicId) {
        return appointmentsUseCase.listCompletedByClinic(clinicId).stream()
                .map(this::toSnapshot)
                .toList();
    }

    private CompletedAppointmentSnapshot toSnapshot(Appointment appointment) {
        return new CompletedAppointmentSnapshot(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getDoctorStaffId(),
                appointment.getQuotationId(),
                appointment.getQuotationItemId(),
                appointment.getQuotationItemIds(),
                appointment.getReason(),
                appointment.getUpdatedAt()
        );
    }
}
