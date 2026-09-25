package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DoctorDirectoryAdapter implements DoctorDirectoryPort {

    public DoctorDirectoryAdapter(ManageAppointmentsUseCase appointments) {
    }

    @Override
    public List<DoctorContact> listDoctors(UUID clinicId) {
        return List.of();
    }

    @Override
    public Optional<DoctorContact> lastDoctorOf(UUID clinicId, UUID patientId) {
        return Optional.empty();
    }
}
