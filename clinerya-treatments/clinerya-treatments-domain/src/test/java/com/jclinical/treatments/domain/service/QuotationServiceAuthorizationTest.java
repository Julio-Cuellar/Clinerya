package com.jclinical.treatments.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.model.QuotationStatus;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase.CreateQuotationCommand;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase.UpdateHeaderCommand;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort;
import com.jclinical.treatments.domain.ports.out.PatientValidatorPort;
import com.jclinical.treatments.domain.ports.out.QuotationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regresion del hallazgo B3: los endpoints de cotizaciones no comprobaban quien llamaba,
 * asi que cualquier usuario autenticado podia leer y modificar las cotizaciones de
 * pacientes de otras clinicas.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuotationServiceAuthorizationTest {

    @Mock
    private QuotationRepositoryPort quotationRepository;

    @Mock
    private PatientValidatorPort patientValidator;

    @Mock
    private InventoryMaterialPort inventoryMaterialPort;

    @Mock
    private PatientAccessAuthorizationPort accessAuthorizationPort;

    private QuotationService service;

    private UUID patientId;
    private UUID clinicId;
    private UUID quotationId;
    private UUID actingUserId;

    @BeforeEach
    void setUp() {
        service = new QuotationService(
                quotationRepository, patientValidator, inventoryMaterialPort, accessAuthorizationPort);
        patientId = UUID.randomUUID();
        clinicId = UUID.randomUUID();
        quotationId = UUID.randomUUID();
        actingUserId = UUID.randomUUID();
        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
    }

    private void givenAccess(AccessLevel level, boolean viaExternalGrant) {
        when(accessAuthorizationPort.resolveAccess(actingUserId, clinicId, patientId))
                .thenReturn(new AccessDecision(level, viaExternalGrant));
    }

    @Test
    void deniesReadsToCallerOutsideTheClinic() {
        givenAccess(AccessLevel.NONE, false);

        assertThrows(ClinicAccessDeniedException.class,
                () -> service.getQuotationsByPatient(patientId, clinicId, actingUserId));
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.getQuotation(quotationId, patientId, clinicId, actingUserId));
        verify(quotationRepository, never()).findByPatientIdAndClinicIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void deniesWritesToCallerOutsideTheClinic() {
        givenAccess(AccessLevel.NONE, false);

        assertThrows(ClinicAccessDeniedException.class, () -> service.createQuotation(
                patientId, clinicId, actingUserId,
                new CreateQuotationCommand(LocalDate.now(), null, null, List.of())));
        assertThrows(ClinicAccessDeniedException.class, () -> service.transitionStatus(
                quotationId, patientId, clinicId, actingUserId, QuotationStatus.SENT));
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.deleteQuotation(quotationId, patientId, clinicId, actingUserId));
        verify(quotationRepository, never()).save(any());
    }

    @Test
    void allowsReadButDeniesWriteToExternalSpecialistWithReadOnlyGrant() {
        givenAccess(AccessLevel.READ_ONLY, true);
        when(quotationRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId))
                .thenReturn(List.of());

        assertTrue(service.getQuotationsByPatient(patientId, clinicId, actingUserId).isEmpty());

        assertThrows(ClinicAccessDeniedException.class, () -> service.updateQuotationHeader(
                quotationId, patientId, clinicId, actingUserId,
                new UpdateHeaderCommand("nota", null, null)));
        verify(quotationRepository, never()).save(any());
    }

    @Test
    void deniesEverythingWhenThereIsNoActingUser() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.getQuotationsByPatient(patientId, clinicId, null));
    }

    @Test
    void recordsTheAuthenticatedUserAsAuthorNotTheRequestBody() {
        givenAccess(AccessLevel.READ_WRITE, false);
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(call -> call.getArgument(0));

        Quotation created = service.createQuotation(
                patientId, clinicId, actingUserId,
                new CreateQuotationCommand(LocalDate.now(), "nota", null, List.of()));

        assertEquals(actingUserId, created.getCreatedByUserId());
    }

    @Test
    void systemLookupSkipsAuthorizationForCrossModuleReads() {
        when(quotationRepository.findByIdAndPatientIdAndClinicId(quotationId, patientId, clinicId))
                .thenReturn(Optional.empty());

        // Agenda y caja validan contra la cotizacion sin usuario en la peticion.
        assertTrue(service.findQuotationForSystem(quotationId, patientId, clinicId).isEmpty());
        verify(accessAuthorizationPort, never()).resolveAccess(any(), any(), any());
    }
}
