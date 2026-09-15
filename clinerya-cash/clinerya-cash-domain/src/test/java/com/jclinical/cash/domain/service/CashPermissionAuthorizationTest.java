package com.jclinical.cash.domain.service;

import com.jclinical.cash.domain.model.PaymentMethod;
import com.jclinical.cash.domain.ports.in.ManageCashSessionUseCase.OpenSessionCommand;
import com.jclinical.cash.domain.ports.in.ManageTicketsUseCase.PaymentLineCommand;
import com.jclinical.cash.domain.ports.in.ManageTicketsUseCase.RegisterTicketCommand;
import com.jclinical.cash.domain.ports.out.CashBankAccountValidatorPort;
import com.jclinical.cash.domain.ports.out.CashPatientValidatorPort;
import com.jclinical.cash.domain.ports.out.CashQuotationValidatorPort;
import com.jclinical.cash.domain.ports.out.CashSessionRepositoryPort;
import com.jclinical.cash.domain.ports.out.CashStaffValidatorPort;
import com.jclinical.cash.domain.ports.out.TicketRepositoryPort;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Regresion de P3: antes de esta fase, ClinicAccessInterceptor solo comprobaba
 * "eres staff activo de esta clinica" sin mirar el permiso. Cualquier miembro activo
 * podia abrir cajas, cobrar y anular tickets. Ahora la autorizacion vive en el dominio
 * y exige el permiso especifico de StaffPermission.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CashPermissionAuthorizationTest {

    @Mock
    private CashSessionRepositoryPort cashSessionRepository;
    @Mock
    private TicketRepositoryPort ticketRepository;
    @Mock
    private CashStaffValidatorPort staffValidator;
    @Mock
    private CashPatientValidatorPort patientValidator;
    @Mock
    private CashQuotationValidatorPort quotationValidator;
    @Mock
    private CashBankAccountValidatorPort bankAccountValidator;
    @Mock
    private DomainEventPublisherPort eventPublisher;
    @Mock
    private StaffPermissionCheckerPort permissionChecker;

    private CashSessionService sessionService;
    private TicketService ticketService;

    private UUID clinicId;
    private UUID actingUserId;

    @BeforeEach
    void setUp() {
        clinicId = UUID.randomUUID();
        actingUserId = UUID.randomUUID();
        sessionService = new CashSessionService(cashSessionRepository, ticketRepository, staffValidator, null, permissionChecker);
        ticketService = new TicketService(
                ticketRepository, cashSessionRepository, patientValidator, quotationValidator,
                staffValidator, bankAccountValidator, eventPublisher, permissionChecker);
    }

    @Test
    void deniesOpeningSessionWithoutManageCashCutsPermission() {
        OpenSessionCommand command = new OpenSessionCommand(UUID.randomUUID(), BigDecimal.TEN);

        assertThrows(ClinicAccessDeniedException.class,
                () -> sessionService.openSession(actingUserId, clinicId, command));

        verify(cashSessionRepository, never()).save(any());
    }

    @Test
    void deniesRegisteringTicketWithoutCreateChargesPermission() {
        RegisterTicketCommand command = new RegisterTicketCommand(
                UUID.randomUUID(), null, "Consulta", UUID.randomUUID(),
                List.of(new PaymentLineCommand(PaymentMethod.CASH, BigDecimal.TEN, null, null)),
                BigDecimal.ZERO, null, null);

        assertThrows(ClinicAccessDeniedException.class,
                () -> ticketService.registerTicket(actingUserId, clinicId, command));

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void deniesVoidingTicketWithoutManageRefundsPermission() {
        UUID ticketId = UUID.randomUUID();

        assertThrows(ClinicAccessDeniedException.class,
                () -> ticketService.voidTicket(actingUserId, ticketId, clinicId,
                        new com.jclinical.cash.domain.ports.in.ManageTicketsUseCase.VoidTicketCommand(
                                UUID.randomUUID(), "Duplicado")));

        verify(ticketRepository, never()).findByIdAndClinicId(any(), any());
    }

    @Test
    void deniesEveryOperationWhenActingUserIsNull() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> sessionService.listSessions(null, clinicId));
        verify(permissionChecker, never()).hasPermission(any(), any(), any());
    }

    @Test
    void allowsOpeningSessionWhenPermissionGranted() {
        UUID staffId = UUID.randomUUID();
        com.jclinical.cash.domain.ports.out.CashStaffValidatorPort.StaffSnapshot staffSnapshot =
                new com.jclinical.cash.domain.ports.out.CashStaffValidatorPort.StaffSnapshot(staffId, "Cajero");
        org.mockito.Mockito.when(permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.MANAGE_CASH_CUTS))
                .thenReturn(true);
        org.mockito.Mockito.when(staffValidator.findActiveStaff(staffId, clinicId))
                .thenReturn(java.util.Optional.of(staffSnapshot));

        sessionService.openSession(actingUserId, clinicId, new OpenSessionCommand(staffId, BigDecimal.TEN));

        verify(cashSessionRepository).save(any());
    }
}
