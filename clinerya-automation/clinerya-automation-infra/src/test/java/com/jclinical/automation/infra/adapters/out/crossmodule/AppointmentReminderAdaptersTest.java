package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.AppointmentNotConfirmableException;
import com.jclinical.automation.domain.ports.out.ReminderPatientPort.ReminderRecipient;
import com.jclinical.patients.domain.model.ConsentSource;
import com.jclinical.patients.domain.model.ContactConsent;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El recordatorio lee al paciente y confirma la cita por las rutas publicas de pacientes y agenda. */
class AppointmentReminderAdaptersTest {

    private final UUID clinicId = UUID.randomUUID();
    private final GetPatientUseCase patients = mock(GetPatientUseCase.class);
    private final OnlineBookingUseCase agenda = mock(OnlineBookingUseCase.class);

    @Test
    void thePatientComesWithTheirWhatsAppNumberAndConsent() {
        Patient ana = Patient.builder().id(UUID.randomUUID()).clinicId(clinicId).firstName("Ana").lastNamePaterno("López")
                .phone("55 1234-5678")
                .contactConsent(new ContactConsent(true, "v1", ConsentSource.WHATSAPP_CHAT, null, LocalDateTime.now())).build();
        when(patients.getPatientById(ana.getId())).thenReturn(Optional.of(ana));

        assertEquals(Optional.of(new ReminderRecipient("Ana López", "5215512345678", true)),
                new ReminderPatientAdapter(patients).find(clinicId, ana.getId()));
    }

    @Test
    void aPatientWithoutConsentOrFromAnotherClinicIsNotReminded() {
        Patient luis = Patient.builder().id(UUID.randomUUID()).clinicId(clinicId).firstName("Luis").phone("525522223333").build();
        Patient other = Patient.builder().id(UUID.randomUUID()).clinicId(UUID.randomUUID()).firstName("Eva").phone("5511112222").build();
        when(patients.getPatientById(luis.getId())).thenReturn(Optional.of(luis));
        when(patients.getPatientById(other.getId())).thenReturn(Optional.of(other));
        ReminderPatientAdapter adapter = new ReminderPatientAdapter(patients);

        assertEquals(Optional.of(new ReminderRecipient("Luis", "5215522223333", false)), adapter.find(clinicId, luis.getId()));
        assertEquals(Optional.empty(), adapter.find(clinicId, other.getId()));
    }

    @Test
    void confirmingGoesThroughTheAgendaAndARefusalIsFalse() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        AppointmentConfirmationAdapter adapter = new AppointmentConfirmationAdapter(agenda);

        assertTrue(adapter.confirm(clinicId, appointmentId, patientId));
        verify(agenda).confirmByPatient(clinicId, appointmentId, patientId);

        doThrow(new AppointmentNotConfirmableException("ya paso")).when(agenda).confirmByPatient(clinicId, appointmentId, patientId);
        assertFalse(adapter.confirm(clinicId, appointmentId, patientId));
    }
}
