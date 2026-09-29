package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.VisitSummary;
import com.jclinical.automation.domain.ports.out.PatientSnapshotPort.PatientSnapshot;
import com.jclinical.patients.domain.model.ConsentSource;
import com.jclinical.patients.domain.model.ContactConsent;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** La ficha del contacto se arma con las rutas publicas de pacientes y agenda, sin leer sus tablas. */
class PatientSnapshotAdapterTest {

    private final UUID clinicId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final GetPatientUseCase patients = mock(GetPatientUseCase.class);
    private final OnlineBookingUseCase agenda = mock(OnlineBookingUseCase.class);

    @Test
    void thePatientsWithThatPhoneComeWithTheirVisitSummary() {
        LocalDateTime registered = LocalDateTime.of(2025, 3, 1, 9, 0);
        Patient ana = Patient.builder().id(UUID.randomUUID()).clinicId(clinicId).firstName("Ana").lastNamePaterno("López")
                .lastNameMaterno("García").phone("55 1234-5678").dateOfBirth(LocalDate.of(1992, 3, 10)).createdAt(registered)
                .contactConsent(new ContactConsent(true, "v1", ConsentSource.WHATSAPP_CHAT, null, registered)).build();
        Patient other = Patient.builder().id(UUID.randomUUID()).clinicId(clinicId).firstName("Luis").lastNamePaterno("Pérez")
                .phone("5522223333").build();
        when(patients.getPatientsByClinic(clinicId)).thenReturn(List.of(ana, other));
        LocalDateTime next = LocalDateTime.of(2026, 10, 2, 16, 0);
        when(agenda.visitSummary(clinicId, ana.getId()))
                .thenReturn(new VisitSummary(next, doctorId, next.minusDays(51), doctorId, 7, 1, 0));

        List<PatientSnapshot> found = new PatientSnapshotAdapter(patients, agenda).findByPhone(clinicId, "5215512345678");

        assertEquals(List.of(new PatientSnapshot(ana.getId(), "Ana López García", LocalDate.of(1992, 3, 10), registered, true,
                next, doctorId, next.minusDays(51), doctorId, 7, 1, 0)), found);
    }

    @Test
    void aNumberWithoutPatientsHasNoSnapshots() {
        when(patients.getPatientsByClinic(clinicId)).thenReturn(List.of());

        assertEquals(List.of(), new PatientSnapshotAdapter(patients, agenda).findByPhone(clinicId, "13055550142"));
    }
}
