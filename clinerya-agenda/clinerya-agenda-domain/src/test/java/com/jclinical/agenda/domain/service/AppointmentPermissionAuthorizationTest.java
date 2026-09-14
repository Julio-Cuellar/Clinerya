package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase.CreateAppointmentCommand;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.PatientValidatorPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regresion de P3: ClinicAccessInterceptor solo comprobaba pertenencia a la clinica,
 * asi que cualquier miembro activo podia crear, editar o cancelar citas de cualquier
 * otro doctor. Ahora se exigen CREATE_APPOINTMENTS/EDIT_APPOINTMENTS/CANCEL_APPOINTMENTS
 * en el dominio.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentPermissionAuthorizationTest {

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
    @Mock
    private StaffPermissionCheckerPort permissionChecker;

    private AppointmentService service;

    private UUID clinicId;
    private UUID actingUserId;

    @BeforeEach
    void setUp() {
        clinicId = UUID.randomUUID();
        actingUserId = UUID.randomUUID();
        service = new AppointmentService(
                appointmentRepository, clinicScheduleService, patientValidator, staffValidator,
                quotationValidator, reservationSchedulingService, eventPublisher, permissionChecker);
    }

    @Test
    void deniesCreatingAppointmentWithoutCreatePermission() {
        CreateAppointmentCommand command = new CreateAppointmentCommand(
                UUID.randomUUID(), UUID.randomUUID(), null, null,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30), "Consulta", null);

        assertThrows(ClinicAccessDeniedException.class,
                () -> service.createAppointment(actingUserId, clinicId, command));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void deniesCancellingWithoutCancelPermissionEvenIfEditIsGranted() {
        UUID appointmentId = UUID.randomUUID();
        when(permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.EDIT_APPOINTMENTS))
                .thenReturn(true);
        when(permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.CANCEL_APPOINTMENTS))
                .thenReturn(false);

        assertThrows(ClinicAccessDeniedException.class,
                () -> service.transitionStatus(actingUserId, appointmentId, clinicId, AppointmentStatus.CANCELLED, "motivo", null));

        verify(appointmentRepository, never()).findByIdAndClinicId(any(), any());
    }

    @Test
    void deniesReadingAgendaWithoutViewPermission() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.listByClinicRange(actingUserId, clinicId, LocalDateTime.now(), LocalDateTime.now().plusDays(1)));
    }

    @Test
    void nullActingUserIdIsTreatedAsATrustedInternalCallerAndSkipsThePermissionCheck() {
        // Los controladores siempre resuelven un actingUserId real desde el token; null solo
        // lo manda una llamada interna (background/evento), que no pasa por autorizacion.
        service.listDoctors(null, clinicId);
        verify(permissionChecker, never()).hasPermission(any(), any(), any());
    }
}
