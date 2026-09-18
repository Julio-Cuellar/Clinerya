package com.jclinical.treatments.domain.service;

import com.jclinical.core.domain.ClinicSpecialty;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase.CreateQuotationCommand;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase.QuotationItemCommand;
import com.jclinical.treatments.domain.ports.out.ClinicSpecialtyPort;
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

import java.math.BigDecimal;
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
 * El localizador dental sólo existe en clínicas odontológicas. Ocultar el campo en la interfaz no
 * basta: la API sigue siendo alcanzable, así que el dominio tiene que rechazar el dato.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuotationServiceSpecialtyValidationTest {

    @Mock
    private QuotationRepositoryPort quotationRepository;

    @Mock
    private PatientValidatorPort patientValidator;

    @Mock
    private InventoryMaterialPort inventoryMaterialPort;

    @Mock
    private PatientAccessAuthorizationPort accessAuthorizationPort;

    @Mock
    private ClinicSpecialtyPort clinicSpecialtyPort;

    private QuotationService service;

    private UUID patientId;
    private UUID clinicId;
    private UUID actingUserId;

    @BeforeEach
    void setUp() {
        service = new QuotationService(
                quotationRepository, patientValidator, inventoryMaterialPort, accessAuthorizationPort,
                clinicSpecialtyPort);
        patientId = UUID.randomUUID();
        clinicId = UUID.randomUUID();
        actingUserId = UUID.randomUUID();

        when(accessAuthorizationPort.resolveAccess(actingUserId, clinicId, patientId))
                .thenReturn(new AccessDecision(AccessLevel.READ_WRITE, false));
        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(call -> call.getArgument(0));
    }

    private void givenSpecialty(ClinicSpecialty specialty) {
        when(clinicSpecialtyPort.findByClinicId(clinicId)).thenReturn(Optional.of(specialty));
    }

    private CreateQuotationCommand quotationWithTooth(Integer toothNumber) {
        return new CreateQuotationCommand(
                LocalDate.now(), null, null,
                List.of(new QuotationItemCommand(
                        null, "Resina simple", toothNumber, BigDecimal.valueOf(950), List.of(), null)));
    }

    @Test
    void dentalClinicAcceptsToothNumber() {
        givenSpecialty(ClinicSpecialty.ODONTOLOGIA);

        Quotation created = service.createQuotation(patientId, clinicId, actingUserId, quotationWithTooth(21));

        assertEquals(21, created.getItems().get(0).getToothNumber());
    }

    @Test
    void dentalClinicStillRejectsInvalidFdiNotation() {
        givenSpecialty(ClinicSpecialty.ODONTOLOGIA);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createQuotation(patientId, clinicId, actingUserId, quotationWithTooth(99)));

        assertTrue(error.getMessage().contains("FDI"));
        verify(quotationRepository, never()).save(any());
    }

    @Test
    void nonDentalClinicRejectsToothNumber() {
        givenSpecialty(ClinicSpecialty.NUTRICION);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createQuotation(patientId, clinicId, actingUserId, quotationWithTooth(21)));

        assertTrue(error.getMessage().contains("no registra número de diente"));
        verify(quotationRepository, never()).save(any());
    }

    @Test
    void nonDentalClinicAcceptsItemsWithoutTooth() {
        givenSpecialty(ClinicSpecialty.NUTRICION);

        Quotation created = service.createQuotation(patientId, clinicId, actingUserId, quotationWithTooth(null));

        assertEquals(1, created.getItems().size());
    }

    /**
     * Una clínica recién dada de alta que todavía no eligió perfil se comporta como no dental:
     * es preferible pedirle que configure la clínica a aceptar un dato que quizá no aplica.
     */
    @Test
    void unconfiguredClinicRejectsToothNumber() {
        givenSpecialty(ClinicSpecialty.SIN_CONFIGURAR);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createQuotation(patientId, clinicId, actingUserId, quotationWithTooth(21)));

        assertTrue(error.getMessage().contains("no registra número de diente"));
    }

    /** Si la clínica no se puede leer, el localizador no se acepta: se falla cerrado. */
    @Test
    void unknownClinicRejectsToothNumber() {
        when(clinicSpecialtyPort.findByClinicId(clinicId)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createQuotation(patientId, clinicId, actingUserId, quotationWithTooth(21)));

        assertTrue(error.getMessage().contains("no registra número de diente"));
    }
}
