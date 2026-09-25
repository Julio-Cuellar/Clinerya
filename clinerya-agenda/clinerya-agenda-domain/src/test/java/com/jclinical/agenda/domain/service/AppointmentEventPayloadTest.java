package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase.CreateAppointmentCommand;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.PatientValidatorPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;
import com.jclinical.core.events.AppointmentCancelledEvent;
import com.jclinical.core.events.AppointmentRescheduledEvent;
import com.jclinical.core.events.AppointmentScheduledEvent;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Los eventos de cita son el contrato con el modulo de automatizacion: con ellos avisa al paciente
 * y al medico sin consultar la agenda. Por eso deben traer a quien pertenece la cita y, al
 * reprogramar o cancelar, el horario que tenia ("tu cita del martes 16:00 se movio al viernes").
 */
@ExtendWith(MockitoExtension.class)
class AppointmentEventPayloadTest {

    @Mock
    private AppointmentRepositoryPort appointmentRepository;
    @Mock
    private ClinicScheduleService clinicScheduleService;
    @Mock
    private PatientValidatorPort patientValidator;
    @Mock
    private StaffValidatorPort staffValidator;
    @Mock
    private QuotationValidatorPort quotationValidator;
    @Mock
    private MaterialReservationSchedulingService reservationSchedulingService;
    @Mock
    private DomainEventPublisherPort eventPublisher;

    private AppointmentService service;

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(16).withMinute(0).withSecond(0).withNano(0);
    private final LocalDateTime end = start.plusMinutes(45);

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, clinicScheduleService, patientValidator, staffValidator,
                quotationValidator, reservationSchedulingService, eventPublisher, (clinic, user, permission) -> true);
    }

    @Test
    void scheduledEventCarriesThePatient() {
        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(staffValidator.findActiveDoctor(doctorId, clinicId)).thenReturn(Optional.of(new DoctorSnapshot(doctorId, "Dra. B")));
        when(clinicScheduleService.getEffectiveDay(clinicId, start.getDayOfWeek())).thenReturn(ClinicSchedule.builder()
                .clinicId(clinicId).dayOfWeek(start.getDayOfWeek()).open(true)
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(20, 0)).build());
        when(appointmentRepository.findActiveByDoctorAndRange(doctorId, clinicId, start, end)).thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment created = service.createAppointment(clinicId,
                new CreateAppointmentCommand(patientId, doctorId, null, null, start, end, "Revision", null));

        AppointmentScheduledEvent event = captured(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED, AppointmentScheduledEvent.class);
        assertEquals(created.getId(), event.appointmentId());
        assertEquals(patientId, event.patientId());
        assertEquals(doctorId, event.doctorStaffId());
    }

    @Test
    void rescheduledEventCarriesTheOwnersAndThePreviousSlot() {
        Appointment appointment = existingAppointment();
        LocalDateTime newStart = start.plusDays(3);
        LocalDateTime newEnd = newStart.plusMinutes(45);

        // externalImport=true evita simular horario de clinica y traslapes; el evento es el mismo.
        service.rescheduleAppointment(null, appointment.getId(), clinicId, newStart, newEnd, true);

        AppointmentRescheduledEvent event = captured(DomainEventRoutingKeys.APPOINTMENT_RESCHEDULED, AppointmentRescheduledEvent.class);
        assertEquals(patientId, event.patientId());
        assertEquals(doctorId, event.doctorStaffId());
        assertEquals(start, event.previousStart());
        assertEquals(end, event.previousEnd());
        assertEquals(newStart, event.newStart());
        assertEquals(newEnd, event.newEnd());
    }

    @Test
    void cancelledEventCarriesTheOwnersTheSlotAndTheReason() {
        Appointment appointment = existingAppointment();
        UUID cancelledBy = UUID.randomUUID();

        service.transitionStatus(null, appointment.getId(), clinicId, AppointmentStatus.CANCELLED, "Tengo un viaje", cancelledBy);

        AppointmentCancelledEvent event = captured(DomainEventRoutingKeys.APPOINTMENT_CANCELLED, AppointmentCancelledEvent.class);
        assertEquals(patientId, event.patientId());
        assertEquals(doctorId, event.doctorStaffId());
        assertEquals(start, event.scheduledStart());
        assertEquals("Tengo un viaje", event.cancellationReason());
    }

    private Appointment existingAppointment() {
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .patientId(patientId)
                .doctorStaffId(doctorId)
                .scheduledStart(start)
                .scheduledEnd(end)
                .status(AppointmentStatus.SCHEDULED)
                .build();
        when(appointmentRepository.findByIdAndClinicId(appointment.getId(), clinicId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return appointment;
    }

    private <T> T captured(String routingKey, Class<T> type) {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eq(routingKey), payload.capture());
        return type.cast(payload.getValue());
    }
}
