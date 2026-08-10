package com.jclinical.cash.domain.service;

import com.jclinical.cash.domain.ports.out.CashAppointmentPort;
import com.jclinical.cash.domain.ports.out.CashAppointmentPort.CompletedAppointmentSnapshot;
import com.jclinical.cash.domain.ports.out.CashQuotationValidatorPort;
import com.jclinical.cash.domain.ports.out.CashQuotationValidatorPort.QuotationItemSnapshot;
import com.jclinical.cash.domain.ports.out.CashQuotationValidatorPort.QuotationSnapshot;
import com.jclinical.cash.domain.ports.out.TicketRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingAppointmentChargeServiceTest {

    @Mock
    private CashAppointmentPort appointmentPort;
    @Mock
    private CashQuotationValidatorPort quotationValidator;
    @Mock
    private TicketRepositoryPort ticketRepository;

    private PendingAppointmentChargeService service;

    @BeforeEach
    void setUp() {
        service = new PendingAppointmentChargeService(appointmentPort, quotationValidator, ticketRepository);
    }

    @Test
    void doesNotCreatePendingChargeWhenQuotationIsFullyPaid() {
        UUID clinicId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        CompletedAppointmentSnapshot appointment = appointment(patientId, quotationId, itemId, 1);

        when(appointmentPort.listCompletedAppointments(clinicId)).thenReturn(List.of(appointment));
        when(quotationValidator.findQuotation(quotationId, patientId, clinicId)).thenReturn(Optional.of(
                new QuotationSnapshot(
                        quotationId,
                        new BigDecimal("1000.00"),
                        true,
                        List.of(new QuotationItemSnapshot(itemId, "Endodoncia", new BigDecimal("700.00"))))));
        when(ticketRepository.sumActiveAmountByQuotation(quotationId, clinicId))
                .thenReturn(new BigDecimal("1000.00"));

        assertTrue(service.listPendingCharges(clinicId).isEmpty());
    }

    @Test
    void allocatesOnlyTheRemainingQuotationBalanceAcrossCompletedAppointments() {
        UUID clinicId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        UUID firstItemId = UUID.randomUUID();
        UUID secondItemId = UUID.randomUUID();
        CompletedAppointmentSnapshot first = appointment(patientId, quotationId, firstItemId, 1);
        CompletedAppointmentSnapshot second = appointment(patientId, quotationId, secondItemId, 2);

        when(appointmentPort.listCompletedAppointments(clinicId)).thenReturn(List.of(second, first));
        when(quotationValidator.findQuotation(quotationId, patientId, clinicId)).thenReturn(Optional.of(
                new QuotationSnapshot(
                        quotationId,
                        new BigDecimal("1000.00"),
                        true,
                        List.of(
                                new QuotationItemSnapshot(firstItemId, "Limpieza", new BigDecimal("300.00")),
                                new QuotationItemSnapshot(secondItemId, "Resina", new BigDecimal("400.00"))))));
        when(ticketRepository.sumActiveAmountByQuotation(quotationId, clinicId))
                .thenReturn(new BigDecimal("500.00"));

        var result = service.listPendingCharges(clinicId);

        assertEquals(1, result.size());
        assertEquals(second.appointmentId(), result.get(0).appointmentId());
        assertEquals(new BigDecimal("200.00"), result.get(0).amount());
    }

    @Test
    void ignoresCompletedAppointmentsWithoutQuotationItem() {
        UUID clinicId = UUID.randomUUID();
        when(appointmentPort.listCompletedAppointments(clinicId)).thenReturn(List.of(
                new CompletedAppointmentSnapshot(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null,
                        "Consulta general", LocalDateTime.now())));

        assertTrue(service.listPendingCharges(clinicId).isEmpty());
    }

    private CompletedAppointmentSnapshot appointment(
            UUID patientId,
            UUID quotationId,
            UUID quotationItemId,
            int completedDay) {
        return new CompletedAppointmentSnapshot(
                UUID.randomUUID(),
                patientId,
                UUID.randomUUID(),
                quotationId,
                quotationItemId,
                "Procedimiento",
                LocalDateTime.of(2026, 7, completedDay, 12, 0));
    }
}
