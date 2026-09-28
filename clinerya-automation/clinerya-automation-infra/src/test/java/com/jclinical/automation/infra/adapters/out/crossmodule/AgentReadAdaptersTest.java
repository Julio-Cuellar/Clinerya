package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.UpcomingAppointment;
import com.jclinical.automation.domain.ports.out.AppointmentCancellationPort;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort.UpcomingVisit;
import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort.CatalogTreatment;
import com.jclinical.treatments.domain.ports.in.PublicTreatmentCatalogUseCase.PublicTreatment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El agente lee el catalogo y las citas por las rutas publicas de tratamientos y agenda, sin reglas propias. */
class AgentReadAdaptersTest {

    private final UUID clinicId = UUID.randomUUID();

    @Test
    void theCatalogIsWhatTheTreatmentsModulePublishes() {
        TreatmentCatalogAdapter adapter = new TreatmentCatalogAdapter(clinic -> List.of(
                new PublicTreatment("Limpieza dental", "Preventivo", new BigDecimal("650.00")),
                new PublicTreatment("Resina", null, null)));

        assertEquals(List.of(new CatalogTreatment("Limpieza dental", "Preventivo", new BigDecimal("650.00")),
                new CatalogTreatment("Resina", null, null)), adapter.activeTreatments(clinicId));
    }

    @Test
    void upcomingVisitsAreTheAgendasUpcomingAppointments() {
        OnlineBookingUseCase agenda = mock(OnlineBookingUseCase.class);
        UUID patientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 9, 28, 10, 0);
        when(agenda.upcomingAppointments(clinicId, patientId, 5))
                .thenReturn(List.of(new UpcomingAppointment(appointmentId, doctorId, start, start.plusMinutes(30), true)));

        List<UpcomingVisit> visits = new PatientAppointmentsAdapter(agenda).upcoming(clinicId, patientId, 5);

        assertEquals(List.of(new UpcomingVisit(appointmentId, doctorId, start, start.plusMinutes(30), true)), visits);
    }

    @Test
    void thePatientCancelsThroughTheAgendasInternalRoute() {
        OnlineBookingUseCase agenda = mock(OnlineBookingUseCase.class);
        UUID patientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        new AppointmentCancellationAdapter(agenda).cancel(clinicId, appointmentId, patientId, "me surgió un viaje");

        verify(agenda).cancelByPatient(clinicId, appointmentId, patientId, "me surgió un viaje");
    }

    @Test
    void anAppointmentTheAgendaWontCancelIsReportedInTheAgentsTerms() {
        OnlineBookingUseCase agenda = mock(OnlineBookingUseCase.class);
        UUID patientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        doThrow(new OnlineBookingUseCase.AppointmentNotCancellableException("ya pasó"))
                .when(agenda).cancelByPatient(clinicId, appointmentId, patientId, null);

        assertThrows(AppointmentCancellationPort.NotCancellableException.class,
                () -> new AppointmentCancellationAdapter(agenda).cancel(clinicId, appointmentId, patientId, null));
    }
}
