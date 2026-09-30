package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.AppointmentNotConfirmableException;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsPort;
import com.jclinical.agenda.domain.ports.out.PatientValidatorPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort;
import com.jclinical.agenda.domain.ports.out.SlotHoldRepositoryPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.core.events.AppointmentConfirmedEvent;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plan v2, S6: el paciente confirma su cita desde el recordatorio de WhatsApp. Solo su propia cita,
 * vigente y futura; confirmar publica appointment.confirmed para quien lo consuma (calendario).
 */
class AppointmentConfirmationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 9, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();

    @Test
    void confirmingAnAppointmentPublishesAppointmentConfirmed() {
        AppointmentRepositoryPort repository = mock(AppointmentRepositoryPort.class);
        DomainEventPublisherPort events = mock(DomainEventPublisherPort.class);
        Appointment appointment = appointment(NOW.plusDays(1), AppointmentStatus.SCHEDULED);
        when(repository.findByIdAndClinicId(appointment.getId(), clinicId)).thenReturn(Optional.of(appointment));
        when(repository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppointmentService service = new AppointmentService(repository, mock(ClinicScheduleService.class),
                mock(PatientValidatorPort.class), mock(StaffValidatorPort.class), mock(QuotationValidatorPort.class),
                mock(MaterialReservationSchedulingService.class), events, (clinic, user, permission) -> true);

        service.transitionStatus(appointment.getId(), clinicId, AppointmentStatus.CONFIRMED, null, null);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(events).publish(eq(DomainEventRoutingKeys.APPOINTMENT_CONFIRMED), payload.capture());
        AppointmentConfirmedEvent event = (AppointmentConfirmedEvent) payload.getValue();
        assertEquals(appointment.getId(), event.appointmentId());
        assertEquals(patientId, event.patientId());
        assertEquals(doctorId, event.doctorStaffId());
    }

    @Test
    void thePatientConfirmsTheirOwnUpcomingAppointment() {
        AppointmentRepositoryPort repository = mock(AppointmentRepositoryPort.class);
        ManageAppointmentsUseCase creator = mock(ManageAppointmentsUseCase.class);
        Appointment appointment = appointment(NOW.plusDays(1), AppointmentStatus.SCHEDULED);
        when(repository.findByIdAndClinicId(appointment.getId(), clinicId)).thenReturn(Optional.of(appointment));

        booking(repository, creator).confirmByPatient(clinicId, appointment.getId(), patientId);

        verify(creator).transitionStatus(appointment.getId(), clinicId, AppointmentStatus.CONFIRMED, null, null);
    }

    @Test
    void anAlreadyConfirmedAppointmentStaysConfirmedWithoutChanges() {
        AppointmentRepositoryPort repository = mock(AppointmentRepositoryPort.class);
        ManageAppointmentsUseCase creator = mock(ManageAppointmentsUseCase.class);
        Appointment appointment = appointment(NOW.plusDays(1), AppointmentStatus.CONFIRMED);
        when(repository.findByIdAndClinicId(appointment.getId(), clinicId)).thenReturn(Optional.of(appointment));

        booking(repository, creator).confirmByPatient(clinicId, appointment.getId(), patientId);

        verify(creator, never()).transitionStatus(any(), any(), any(), any(), any());
    }

    @Test
    void someoneElsesPastOrCancelledAppointmentCannotBeConfirmed() {
        AppointmentRepositoryPort repository = mock(AppointmentRepositoryPort.class);
        ManageAppointmentsUseCase creator = mock(ManageAppointmentsUseCase.class);
        Appointment past = appointment(NOW.minusHours(1), AppointmentStatus.SCHEDULED);
        Appointment cancelled = appointment(NOW.plusDays(1), AppointmentStatus.CANCELLED);
        when(repository.findByIdAndClinicId(past.getId(), clinicId)).thenReturn(Optional.of(past));
        when(repository.findByIdAndClinicId(cancelled.getId(), clinicId)).thenReturn(Optional.of(cancelled));
        OnlineBookingService service = booking(repository, creator);

        assertThrows(AppointmentNotConfirmableException.class, () -> service.confirmByPatient(clinicId, past.getId(), patientId));
        assertThrows(AppointmentNotConfirmableException.class,
                () -> service.confirmByPatient(clinicId, cancelled.getId(), patientId));
        assertThrows(AppointmentNotConfirmableException.class,
                () -> service.confirmByPatient(clinicId, cancelled.getId(), UUID.randomUUID()));
        verify(creator, never()).transitionStatus(any(), any(), any(), any(), any());
    }

    private OnlineBookingService booking(AppointmentRepositoryPort repository, ManageAppointmentsUseCase creator) {
        return new OnlineBookingService(mock(ClinicScheduleService.class), repository, mock(SlotHoldRepositoryPort.class),
                mock(OnlineBookingSettingsPort.class), mock(StaffValidatorPort.class), creator,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private Appointment appointment(LocalDateTime start, AppointmentStatus status) {
        return Appointment.builder().id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId).doctorStaffId(doctorId)
                .scheduledStart(start).scheduledEnd(start.plusMinutes(30)).status(status).build();
    }
}
