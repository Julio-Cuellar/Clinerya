package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase.CreateAppointmentCommand;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.PatientValidatorPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort.AcceptedQuotationSnapshot;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort.QuotationItemSnapshot;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;
import com.jclinical.core.events.DomainEventPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

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

    @BeforeEach
    void setUp() {
        service = new AppointmentService(
                appointmentRepository,
                clinicScheduleService,
                patientValidator,
                staffValidator,
                quotationValidator,
                reservationSchedulingService,
                eventPublisher
        );
    }

    @Test
    void createAppointment_attemptsMaterialReservationImmediately() {
        UUID clinicId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusMinutes(45);

        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(staffValidator.findActiveDoctor(doctorId, clinicId))
                .thenReturn(Optional.of(new DoctorSnapshot(doctorId, "Doctora Demo")));
        when(clinicScheduleService.getEffectiveDay(clinicId, start.getDayOfWeek()))
                .thenReturn(ClinicSchedule.builder()
                        .clinicId(clinicId)
                        .dayOfWeek(start.getDayOfWeek())
                        .open(true)
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(20, 0))
                        .build());
        when(quotationValidator.findAcceptedQuotation(quotationId, patientId, clinicId))
                .thenReturn(Optional.of(new AcceptedQuotationSnapshot(
                        quotationId, List.of(new QuotationItemSnapshot(itemId, "Endodoncia", List.of())))));
        when(appointmentRepository.findActiveByDoctorAndRange(doctorId, clinicId, start, end))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Appointment created = service.createAppointment(clinicId, new CreateAppointmentCommand(
                patientId, doctorId, quotationId, itemId, start, end, "Endodoncia", null));

        assertEquals(quotationId, created.getQuotationId());
        assertEquals(itemId, created.getQuotationItemId());
        verify(reservationSchedulingService).processReservation(eq(created));
    }
}
