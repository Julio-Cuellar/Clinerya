package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.UpcomingAppointment;
import com.jclinical.automation.domain.ports.out.AppointmentCancellationPort;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort.UpcomingVisit;
import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort.CatalogTreatment;
import com.jclinical.treatments.domain.model.PricingType;
import com.jclinical.treatments.domain.ports.in.PublicTreatmentCatalogUseCase;
import com.jclinical.treatments.domain.ports.in.PublicTreatmentCatalogUseCase.PublicService;
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
    void theAgentOnlySeesTheServicesTheTreatmentsModuleOffersForTheAssistant() {
        PublicTreatmentCatalogUseCase catalog = mock(PublicTreatmentCatalogUseCase.class);
        UUID cleaning = UUID.randomUUID();
        UUID braces = UUID.randomUUID();
        when(catalog.assistantServices(clinicId)).thenReturn(List.of(
                new PublicService(cleaning, "Limpieza dental", "Preventivo", "Retiramos sarro y placa con ultrasonido.",
                        PricingType.FIXED, new BigDecimal("650.00"), 45),
                new PublicService(braces, "Ortodoncia", null, "Brackets; el plan se define en la valoración.",
                        PricingType.VARIES_BY_PATIENT, null, 60)));

        assertEquals(List.of(
                new CatalogTreatment(cleaning, "Limpieza dental", "Preventivo", "Retiramos sarro y placa con ultrasonido.",
                        true, new BigDecimal("650.00"), 45),
                new CatalogTreatment(braces, "Ortodoncia", null, "Brackets; el plan se define en la valoración.", false, null, 60)),
                new TreatmentCatalogAdapter(catalog).activeTreatments(clinicId));
    }

    @Test
    void slotsForAServiceAreAskedWithItsDuration() {
        OnlineBookingUseCase agenda = mock(OnlineBookingUseCase.class);
        UUID doctorId = UUID.randomUUID();
        java.time.LocalDate from = java.time.LocalDate.of(2026, 10, 1);
        LocalDateTime start = from.atTime(9, 0);
        when(agenda.findAvailableSlots(clinicId, doctorId, from, 7, 10, 45))
                .thenReturn(List.of(new com.jclinical.agenda.domain.model.BookableSlot(start, start.plusMinutes(45))));

        List<com.jclinical.automation.domain.model.AvailableSlot> slots =
                new SlotAvailabilityAdapter(agenda).availableSlots(clinicId, doctorId, from, 7, 10, 45);

        assertEquals(List.of(new com.jclinical.automation.domain.model.AvailableSlot(start, start.plusMinutes(45))), slots);
    }

    @Test
    void bookingAHeldSlotForAServicePassesTheServiceToTheAgenda() {
        com.jclinical.agenda.domain.service.OnlineBookingService agenda =
                mock(com.jclinical.agenda.domain.service.OnlineBookingService.class);
        UUID holdId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        when(agenda.bookHeldSlot(new OnlineBookingUseCase.BookHeldSlotCommand(clinicId, holdId, patientId, "WhatsApp", serviceId)))
                .thenReturn(appointmentId);

        assertEquals(appointmentId, new SlotBookingAdapter(agenda).book(clinicId, holdId, patientId, "WhatsApp", serviceId));
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
