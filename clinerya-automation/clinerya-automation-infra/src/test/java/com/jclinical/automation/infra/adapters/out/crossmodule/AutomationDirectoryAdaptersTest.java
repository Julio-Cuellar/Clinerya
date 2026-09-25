package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort.DoctorContact;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Adaptadores de lectura hacia pacientes y agenda. WhatsApp entrega el celular en formato
 * internacional (5215512345678) y la clinica lo captura como lo dicta el paciente (55 1234 5678):
 * se comparan los ultimos 10 digitos.
 */
class AutomationDirectoryAdaptersTest {

    private final UUID clinicId = UUID.randomUUID();
    private final GetPatientUseCase getPatients = mock(GetPatientUseCase.class);
    private final ManageAppointmentsUseCase appointments = mock(ManageAppointmentsUseCase.class);
    private final PatientDirectoryAdapter patientDirectory = new PatientDirectoryAdapter(getPatients);
    private final DoctorDirectoryAdapter doctorDirectory = new DoctorDirectoryAdapter(appointments);

    @Test
    void findsThePatientWhateverFormatThePhoneWasCapturedIn() {
        Patient ana = patient("Ana", "López", "55 1234-5678");
        Patient other = patient("Otro", "Paciente", "5587654321");
        Patient noPhone = patient("Sin", "Telefono", null);
        when(getPatients.getPatientsByClinic(clinicId)).thenReturn(List.of(ana, other, noPhone));

        List<PatientContact> found = patientDirectory.findByPhone(clinicId, "5215512345678");

        assertEquals(List.of(new PatientContact(ana.getId(), "Ana López")), found);
    }

    @Test
    void aFamilySharingAPhoneIsReturnedTogether() {
        Patient mother = patient("Ana", "López", "5512345678");
        Patient son = patient("Luis", "López", "+52 55 1234 5678");
        when(getPatients.getPatientsByClinic(clinicId)).thenReturn(List.of(mother, son));

        assertEquals(2, patientDirectory.findByPhone(clinicId, "525512345678").size());
    }

    @Test
    void aTooShortPhoneMatchesNobody() {
        when(getPatients.getPatientsByClinic(clinicId)).thenReturn(List.of(patient("Ana", "López", "12345")));

        assertTrue(patientDirectory.findByPhone(clinicId, "12345").isEmpty());
    }

    @Test
    void listsThePractitionersOfTheClinic() {
        UUID doctorId = UUID.randomUUID();
        when(appointments.listDoctors(null, clinicId)).thenReturn(List.of(new DoctorSnapshot(doctorId, "Dra. Beatriz Ramos")));

        assertEquals(List.of(new DoctorContact(doctorId, "Dra. Beatriz Ramos")), doctorDirectory.listDoctors(clinicId));
    }

    @Test
    void theLastDoctorComesFromTheMostRecentCompletedAppointment() {
        UUID patientId = UUID.randomUUID();
        UUID oldDoctor = UUID.randomUUID();
        UUID recentDoctor = UUID.randomUUID();
        UUID futureDoctor = UUID.randomUUID();
        when(appointments.listByPatient(null, patientId, clinicId)).thenReturn(List.of(
                appointment(patientId, oldDoctor, AppointmentStatus.COMPLETED, LocalDateTime.of(2026, 1, 10, 10, 0)),
                appointment(patientId, recentDoctor, AppointmentStatus.COMPLETED, LocalDateTime.of(2026, 8, 2, 10, 0)),
                appointment(patientId, futureDoctor, AppointmentStatus.SCHEDULED, LocalDateTime.of(2026, 10, 1, 10, 0))));
        when(appointments.listDoctors(null, clinicId)).thenReturn(List.of(
                new DoctorSnapshot(oldDoctor, "Dr. Antiguo"), new DoctorSnapshot(recentDoctor, "Dra. Reciente"),
                new DoctorSnapshot(futureDoctor, "Dr. Futuro")));

        assertEquals(Optional.of(new DoctorContact(recentDoctor, "Dra. Reciente")), doctorDirectory.lastDoctorOf(clinicId, patientId));
    }

    @Test
    void aLastDoctorWhoNoLongerAttendsIsNotOffered() {
        UUID patientId = UUID.randomUUID();
        UUID formerDoctor = UUID.randomUUID();
        when(appointments.listByPatient(null, patientId, clinicId)).thenReturn(List.of(
                appointment(patientId, formerDoctor, AppointmentStatus.COMPLETED, LocalDateTime.of(2026, 8, 2, 10, 0))));
        when(appointments.listDoctors(null, clinicId)).thenReturn(List.of());

        assertTrue(doctorDirectory.lastDoctorOf(clinicId, patientId).isEmpty());
    }

    private Patient patient(String firstName, String lastName, String phone) {
        return Patient.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .firstName(firstName)
                .lastNamePaterno(lastName)
                .phone(phone)
                .build();
    }

    private Appointment appointment(UUID patientId, UUID doctorId, AppointmentStatus status, LocalDateTime start) {
        return Appointment.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .patientId(patientId)
                .doctorStaffId(doctorId)
                .status(status)
                .scheduledStart(start)
                .scheduledEnd(start.plusMinutes(45))
                .build();
    }
}
