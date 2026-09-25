package com.jclinical.agenda.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Para la agenda un "doctor" es quien atiende pacientes: el rol DOCTOR o un administrador que
 * ademas atiende (el dueno de la clinica, casi siempre). Filtrar por rol dejaba fuera al dueno.
 */
@Component
@RequiredArgsConstructor
public class AgendaStaffValidatorAdapter implements StaffValidatorPort {

    private final ManageClinicStaffUseCase clinicStaffUseCase;

    @Override
    public Optional<DoctorSnapshot> findActiveDoctor(UUID staffId, UUID clinicId) {
        return clinicStaffUseCase.getActiveStaffById(staffId, clinicId)
                .filter(ManageClinicStaffUseCase.StaffSummary::practitioner)
                .map(staff -> new DoctorSnapshot(staff.staffId(), staff.fullName()));
    }

    @Override
    public List<DoctorSnapshot> listActiveDoctors(UUID clinicId) {
        return clinicStaffUseCase.listPractitioners(clinicId).stream()
                .map(staff -> new DoctorSnapshot(staff.staffId(), staff.fullName()))
                .toList();
    }
}
