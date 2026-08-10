package com.jclinical.agenda.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgendaStaffValidatorAdapter implements StaffValidatorPort {

    private final ManageClinicStaffUseCase clinicStaffUseCase;

    @Override
    public Optional<DoctorSnapshot> findActiveDoctor(UUID staffId, UUID clinicId) {
        return clinicStaffUseCase.getActiveStaffById(staffId, clinicId)
                .filter(staff -> staff.role() == StaffRole.DOCTOR)
                .map(staff -> new DoctorSnapshot(staff.staffId(), staff.fullName()));
    }

    @Override
    public List<DoctorSnapshot> listActiveDoctors(UUID clinicId) {
        return clinicStaffUseCase.listStaffByClinic(clinicId, StaffRole.DOCTOR).stream()
                .map(staff -> new DoctorSnapshot(staff.staffId(), staff.fullName()))
                .toList();
    }
}
