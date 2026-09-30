package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.model.ServicePricing;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase.CreateAppointmentCommand;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.PatientValidatorPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort;
import com.jclinical.agenda.domain.ports.out.ServiceCatalogPort.ServiceSnapshot;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;
import com.jclinical.core.events.AppointmentScheduledEvent;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plan v2, S2: la cita puede llevar un servicio del catalogo. Su duracion fija la hora de fin si no se
 * indica, y su precio fijo se copia a la cita (si el catalogo cambia despues, la cita no). El evento de
 * cita agendada lleva el servicio para quien lo consuma (calendario, recordatorios).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentWithServiceTest {

    @Mock private AppointmentRepositoryPort appointmentRepository;
    @Mock private ClinicScheduleService clinicScheduleService;
    @Mock private PatientValidatorPort patientValidator;
    @Mock private StaffValidatorPort staffValidator;
    @Mock private QuotationValidatorPort quotationValidator;
    @Mock private MaterialReservationSchedulingService reservationSchedulingService;
    @Mock private DomainEventPublisherPort eventPublisher;

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID cleaningId = UUID.randomUUID();
    private final UUID orthodonticsId = UUID.randomUUID();
    private final LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(16).withMinute(0).withSecond(0).withNano(0);
    private final Map<UUID, ServiceSnapshot> catalog = Map.of(
            cleaningId, new ServiceSnapshot(cleaningId, "Limpieza dental", ServicePricing.FIXED, new BigDecimal("650"), 45),
            orthodonticsId, new ServiceSnapshot(orthodonticsId, "Ortodoncia", ServicePricing.VARIES_BY_PATIENT,
                    new BigDecimal("18000"), 60));

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, clinicScheduleService, patientValidator, staffValidator,
                quotationValidator, reservationSchedulingService, eventPublisher, (clinic, user, permission) -> true);
        service.setServiceCatalog((clinic, id) -> clinic.equals(clinicId) ? Optional.ofNullable(catalog.get(id)) : Optional.empty());
        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(staffValidator.findActiveDoctor(doctorId, clinicId)).thenReturn(Optional.of(new DoctorSnapshot(doctorId, "Dra. B")));
        when(clinicScheduleService.getEffectiveDay(clinicId, start.getDayOfWeek())).thenReturn(ClinicSchedule.builder()
                .clinicId(clinicId).dayOfWeek(start.getDayOfWeek()).open(true)
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(20, 0)).build());
        when(appointmentRepository.findActiveByDoctorAndRange(eq(doctorId), eq(clinicId), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void aFixedPriceServiceCopiesItsNameAndPriceAndSetsTheEndByItsDuration() {
        Appointment created = service.createAppointment(clinicId, command(cleaningId, null));

        assertEquals(cleaningId, created.getServiceId());
        assertEquals("Limpieza dental", created.getServiceName());
        assertEquals(ServicePricing.FIXED, created.getServicePricing());
        assertEquals(new BigDecimal("650"), created.getServicePrice());
        assertEquals(start.plusMinutes(45), created.getScheduledEnd());
    }

    @Test
    void aGivenEndIsKeptEvenIfItDiffersFromTheDuration() {
        Appointment created = service.createAppointment(clinicId, command(cleaningId, start.plusMinutes(60)));

        assertEquals(start.plusMinutes(60), created.getScheduledEnd());
    }

    @Test
    void aVariablePriceServiceLeavesThePriceToBeDefined() {
        Appointment created = service.createAppointment(clinicId, command(orthodonticsId, null));

        assertEquals(ServicePricing.VARIES_BY_PATIENT, created.getServicePricing());
        assertNull(created.getServicePrice(), "el precio de referencia no es lo que se cobra");
        assertEquals(start.plusMinutes(60), created.getScheduledEnd());
    }

    @Test
    void anUnknownOrInactiveServiceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.createAppointment(clinicId, command(UUID.randomUUID(), null)));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void withoutServiceTheAppointmentIsAsBefore() {
        Appointment created = service.createAppointment(clinicId, command(null, start.plusMinutes(30)));

        assertNull(created.getServiceId());
        assertNull(created.getServicePrice());
    }

    @Test
    void theScheduledEventCarriesTheService() {
        service.createAppointment(clinicId, command(cleaningId, null));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eq(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED), payload.capture());
        AppointmentScheduledEvent event = (AppointmentScheduledEvent) payload.getValue();
        assertEquals(cleaningId, event.serviceId());
        assertEquals("Limpieza dental", event.serviceName());
        assertEquals(new BigDecimal("650"), event.servicePrice());
        assertEquals("FIXED", event.servicePricing());
    }

    private CreateAppointmentCommand command(UUID serviceId, LocalDateTime end) {
        return new CreateAppointmentCommand(patientId, doctorId, null, null, null, List.of(), start, end, "Cita", null,
                false, serviceId);
    }
}
