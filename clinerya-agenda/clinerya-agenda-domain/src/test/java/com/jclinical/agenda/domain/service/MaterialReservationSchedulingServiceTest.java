package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.ClinicSettingsPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort.AcceptedQuotationSnapshot;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort.QuotationItemMaterialSnapshot;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort.QuotationItemSnapshot;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import com.jclinical.core.events.MaterialReservationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialReservationSchedulingServiceTest {

    @Mock
    private AppointmentRepositoryPort appointmentRepository;
    @Mock
    private QuotationValidatorPort quotationValidator;
    @Mock
    private ClinicSettingsPort clinicSettingsPort;
    @Mock
    private DomainEventPublisherPort eventPublisher;

    private MaterialReservationSchedulingService service;
    private UUID clinicId;
    private UUID patientId;
    private UUID quotationId;
    private UUID quotationItemId;
    private UUID appointmentId;

    @BeforeEach
    void setUp() {
        service = new MaterialReservationSchedulingService(
                appointmentRepository, quotationValidator, clinicSettingsPort, eventPublisher);
        clinicId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        quotationId = UUID.randomUUID();
        quotationItemId = UUID.randomUUID();
        appointmentId = UUID.randomUUID();
    }

    @Test
    void processReservation_reservesImmediatelyWhenAppointmentIsInsideLeadWindow() {
        Appointment appointment = appointment(LocalDateTime.now().plusHours(2));
        UUID glovesId = UUID.randomUUID();
        UUID anestheticId = UUID.randomUUID();
        when(clinicSettingsPort.getMaterialReservationLeadDays(clinicId)).thenReturn(3);
        when(quotationValidator.findAcceptedQuotation(quotationId, patientId, clinicId))
                .thenReturn(Optional.of(new AcceptedQuotationSnapshot(quotationId, List.of(
                        new QuotationItemSnapshot(quotationItemId, "Endodoncia", List.of(
                                new QuotationItemMaterialSnapshot(glovesId, "Guantes", BigDecimal.ONE),
                                new QuotationItemMaterialSnapshot(anestheticId, "Anestesia", BigDecimal.valueOf(2))
                        ))
                ))));

        boolean processed = service.processReservation(appointment);

        assertTrue(processed);
        assertTrue(appointment.isMaterialsReserved());
        verify(appointmentRepository).save(appointment);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eq(DomainEventRoutingKeys.MATERIAL_RESERVATION_REQUESTED), eventCaptor.capture());
        MaterialReservationRequestedEvent event = (MaterialReservationRequestedEvent) eventCaptor.getValue();
        assertEquals(appointmentId, event.appointmentId());
        assertEquals(2, event.materials().size());
        assertEquals(BigDecimal.valueOf(2), event.materials().get(1).quantity());
    }

    @Test
    void processReservation_keepsFutureAppointmentPendingUntilLeadWindow() {
        Appointment appointment = appointment(LocalDateTime.now().plusDays(5));
        when(clinicSettingsPort.getMaterialReservationLeadDays(clinicId)).thenReturn(3);

        boolean processed = service.processReservation(appointment);

        assertFalse(processed);
        assertFalse(appointment.isMaterialsReserved());
        verifyNoInteractions(quotationValidator, eventPublisher);
    }

    @Test
    void processReservation_doesNotMarkAppointmentWhenQuotationCannotBeResolved() {
        Appointment appointment = appointment(LocalDateTime.now().plusHours(2));
        when(clinicSettingsPort.getMaterialReservationLeadDays(clinicId)).thenReturn(3);
        when(quotationValidator.findAcceptedQuotation(quotationId, patientId, clinicId))
                .thenReturn(Optional.empty());

        boolean processed = service.processReservation(appointment);

        assertFalse(processed);
        assertFalse(appointment.isMaterialsReserved());
        verifyNoInteractions(eventPublisher);
    }

    private Appointment appointment(LocalDateTime scheduledStart) {
        return Appointment.builder()
                .id(appointmentId)
                .clinicId(clinicId)
                .patientId(patientId)
                .quotationId(quotationId)
                .quotationItemId(quotationItemId)
                .scheduledStart(scheduledStart)
                .scheduledEnd(scheduledStart.plusMinutes(45))
                .status(AppointmentStatus.SCHEDULED)
                .materialsReserved(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
