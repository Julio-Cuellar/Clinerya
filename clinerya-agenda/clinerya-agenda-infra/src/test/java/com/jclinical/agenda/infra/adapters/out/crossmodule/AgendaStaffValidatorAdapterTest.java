package com.jclinical.agenda.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;
import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase.StaffSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regresion: la agenda filtraba por {@code role == DOCTOR}, asi que el dueno de la clinica
 * (CLINIC_ADMIN desde V29) no podia recibir citas aunque atendiera pacientes.
 */
class AgendaStaffValidatorAdapterTest {

    private final ManageClinicStaffUseCase clinicStaffUseCase = mock(ManageClinicStaffUseCase.class);
    private final AgendaStaffValidatorAdapter adapter = new AgendaStaffValidatorAdapter(clinicStaffUseCase);
    private final UUID clinicId = UUID.randomUUID();

    @Test
    void clinicAdminWhoAttendsPatientsCanBeBooked() {
        StaffSummary owner = staff(StaffRole.CLINIC_ADMIN, "Dra. Duena", true);
        when(clinicStaffUseCase.getActiveStaffById(owner.staffId(), clinicId)).thenReturn(Optional.of(owner));

        Optional<DoctorSnapshot> doctor = adapter.findActiveDoctor(owner.staffId(), clinicId);

        assertTrue(doctor.isPresent());
        assertEquals("Dra. Duena", doctor.get().fullName());
    }

    @Test
    void clinicAdminWhoDoesNotAttendPatientsCannotBeBooked() {
        StaffSummary administrator = staff(StaffRole.CLINIC_ADMIN, "Administrador", false);
        when(clinicStaffUseCase.getActiveStaffById(administrator.staffId(), clinicId)).thenReturn(Optional.of(administrator));

        assertTrue(adapter.findActiveDoctor(administrator.staffId(), clinicId).isEmpty());
    }

    @Test
    void listsEveryPractitionerAsABookableDoctor() {
        StaffSummary doctor = staff(StaffRole.DOCTOR, "Dr. Lopez", true);
        StaffSummary owner = staff(StaffRole.CLINIC_ADMIN, "Dra. Duena", true);
        when(clinicStaffUseCase.listPractitioners(clinicId)).thenReturn(List.of(doctor, owner));

        List<DoctorSnapshot> doctors = adapter.listActiveDoctors(clinicId);

        assertEquals(List.of(doctor.staffId(), owner.staffId()), doctors.stream().map(DoctorSnapshot::staffId).toList());
    }

    @Test
    void resolvesTheUserBehindAStaffMember() {
        StaffSummary doctor = staff(StaffRole.DOCTOR, "Dr. Lopez", true);
        when(clinicStaffUseCase.getActiveStaffById(doctor.staffId(), clinicId)).thenReturn(Optional.of(doctor));

        assertEquals(Optional.of(doctor.userId()), adapter.userIdOfStaff(doctor.staffId(), clinicId));
    }

    private StaffSummary staff(StaffRole role, String fullName, boolean practitioner) {
        return new StaffSummary(UUID.randomUUID(), clinicId, UUID.randomUUID(), role, fullName,
                role != StaffRole.DOCTOR && practitioner, practitioner);
    }
}
