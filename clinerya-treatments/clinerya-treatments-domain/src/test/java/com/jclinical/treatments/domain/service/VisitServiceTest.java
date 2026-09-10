package com.jclinical.treatments.domain.service;

import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.treatments.domain.model.ItemProgressStatus;
import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.model.QuotationItem;
import com.jclinical.treatments.domain.model.QuotationItemMaterial;
import com.jclinical.treatments.domain.model.QuotationStatus;
import com.jclinical.treatments.domain.model.Visit;
import com.jclinical.treatments.domain.model.VisitLineItem;
import com.jclinical.treatments.domain.model.VisitMaterialUsage;
import com.jclinical.treatments.domain.ports.in.ManageVisitsUseCase.RegisterVisitCommand;
import com.jclinical.treatments.domain.ports.in.ManageVisitsUseCase.RegisterVisitLineItemCommand;
import com.jclinical.treatments.domain.ports.in.ManageVisitsUseCase.RegisterVisitMaterialUsageCommand;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort.MaterialSnapshot;
import com.jclinical.treatments.domain.ports.out.PatientValidatorPort;
import com.jclinical.treatments.domain.ports.out.QuotationRepositoryPort;
import com.jclinical.treatments.domain.ports.out.VisitRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitServiceTest {

    @Mock
    private VisitRepositoryPort visitRepository;

    @Mock
    private QuotationRepositoryPort quotationRepository;

    @Mock
    private PatientValidatorPort patientValidator;

    @Mock
    private InventoryMaterialPort inventoryMaterialPort;

    @Mock
    private DomainEventPublisherPort eventPublisher;

    private VisitService visitService;

    private final UUID actingUserId = UUID.randomUUID();
    private final StaffPermissionCheckerPort permissionChecker = (clinicId, userId, permission) -> true;

    private UUID patientId;
    private UUID clinicId;
    private UUID quotationId;
    private UUID quotationItemId;
    private UUID materialId;

    @BeforeEach
    void setUp() {
        visitService = new VisitService(
                visitRepository,
                quotationRepository,
                patientValidator,
                inventoryMaterialPort,
                eventPublisher,
                permissionChecker
        );

        patientId = UUID.randomUUID();
        clinicId = UUID.randomUUID();
        quotationId = UUID.randomUUID();
        quotationItemId = UUID.randomUUID();
        materialId = UUID.randomUUID();
    }

    @Test
    void registerVisit_Success() {
        // Arrange
        LocalDate visitDate = LocalDate.now();
        UUID doctorId = UUID.randomUUID();
        String notes = "Sesión de limpieza dental.";

        RegisterVisitMaterialUsageCommand usageCmd = new RegisterVisitMaterialUsageCommand(
                materialId,
                "Lidocaína",
                BigDecimal.valueOf(2.0)
        );

        RegisterVisitLineItemCommand lineCmd = new RegisterVisitLineItemCommand(
                quotationItemId,
                List.of(usageCmd)
        );

        RegisterVisitCommand command = new RegisterVisitCommand(
                visitDate,
                doctorId,
                notes,
                List.of(lineCmd)
        );

        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);

        QuotationItemMaterial estimatedMat = QuotationItemMaterial.builder()
                .id(UUID.randomUUID())
                .materialId(materialId)
                .materialName("Lidocaína")
                .estimatedQuantity(BigDecimal.valueOf(1.0))
                .unitCostAtQuote(BigDecimal.valueOf(50.0))
                .build();

        QuotationItem qItem = QuotationItem.builder()
                .id(quotationItemId)
                .catalogItemId(UUID.randomUUID())
                .description("Limpieza dental")
                .laborCharge(BigDecimal.valueOf(200.0))
                .materials(List.of(estimatedMat))
                .progressStatus(ItemProgressStatus.PENDING)
                .build();

        Quotation quotation = Quotation.builder()
                .id(quotationId)
                .clinicId(clinicId)
                .patientId(patientId)
                .status(QuotationStatus.ACCEPTED)
                .items(List.of(qItem))
                .build();

        when(quotationRepository.findByIdAndPatientIdAndClinicId(quotationId, patientId, clinicId))
                .thenReturn(Optional.of(quotation));
        when(inventoryMaterialPort.findActiveMaterial(materialId, clinicId))
                .thenReturn(Optional.of(new MaterialSnapshot(materialId, "Lidocaína", BigDecimal.valueOf(50.0))));

        when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Visit registered = visitService.registerVisit(actingUserId, patientId, quotationId, clinicId, command);

        // Assert
        assertNotNull(registered);
        assertEquals(clinicId, registered.getClinicId());
        assertEquals(patientId, registered.getPatientId());
        assertEquals(quotationId, registered.getQuotationId());
        assertEquals(visitDate, registered.getVisitDate());
        assertEquals(doctorId, registered.getDoctorId());
        assertEquals(notes, registered.getNotes());
        assertEquals(1, registered.getItems().size());

        VisitLineItem registeredItem = registered.getItems().get(0);
        assertEquals(quotationItemId, registeredItem.getQuotationItemId());
        assertEquals(1, registeredItem.getMaterialsUsed().size());

        VisitMaterialUsage registeredUsage = registeredItem.getMaterialsUsed().get(0);
        assertEquals(materialId, registeredUsage.getMaterialId());
        assertEquals("Lidocaína", registeredUsage.getMaterialName());
        assertEquals(BigDecimal.valueOf(2.0), registeredUsage.getActualQuantity());

        verify(inventoryMaterialPort).registerUsage(
                eq(clinicId),
                eq(materialId),
                eq(BigDecimal.valueOf(2.0)),
                eq("VISIT"),
                eq(registered.getId()),
                eq(notes)
        );

        verify(visitRepository).save(any(Visit.class));
    }

    @Test
    void registerVisit_PatientNotFound_ThrowsException() {
        // Arrange
        RegisterVisitCommand command = new RegisterVisitCommand(
                LocalDate.now(),
                UUID.randomUUID(),
                "Notas",
                List.of()
        );

        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            visitService.registerVisit(actingUserId, patientId, quotationId, clinicId, command);
        });

        assertEquals("El paciente no existe en esta clínica.", exception.getMessage());
        verifyNoInteractions(quotationRepository, inventoryMaterialPort, visitRepository);
    }

    @Test
    void registerVisit_QuotationNotFound_ThrowsException() {
        // Arrange
        RegisterVisitCommand command = new RegisterVisitCommand(
                LocalDate.now(),
                UUID.randomUUID(),
                "Notas",
                List.of()
        );

        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(quotationRepository.findByIdAndPatientIdAndClinicId(quotationId, patientId, clinicId))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            visitService.registerVisit(actingUserId, patientId, quotationId, clinicId, command);
        });

        assertEquals("La cotización no existe para este paciente en esta clínica.", exception.getMessage());
        verifyNoInteractions(inventoryMaterialPort, visitRepository);
    }

    @Test
    void registerVisit_QuotationNotAccepted_ThrowsException() {
        // Arrange
        RegisterVisitCommand command = new RegisterVisitCommand(
                LocalDate.now(),
                UUID.randomUUID(),
                "Notas",
                List.of()
        );

        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);

        Quotation quotation = Quotation.builder()
                .id(quotationId)
                .clinicId(clinicId)
                .patientId(patientId)
                .status(QuotationStatus.DRAFT)
                .items(List.of())
                .build();

        when(quotationRepository.findByIdAndPatientIdAndClinicId(quotationId, patientId, clinicId))
                .thenReturn(Optional.of(quotation));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            visitService.registerVisit(actingUserId, patientId, quotationId, clinicId, command);
        });

        assertEquals("Solo se pueden registrar sesiones clínicas para cotizaciones aceptadas.", exception.getMessage());
        verifyNoInteractions(inventoryMaterialPort, visitRepository);
    }

    @Test
    void registerVisit_ItemNotBelongToQuotation_ThrowsException() {
        // Arrange
        RegisterVisitLineItemCommand lineCmd = new RegisterVisitLineItemCommand(
                UUID.randomUUID(),
                List.of()
        );

        RegisterVisitCommand command = new RegisterVisitCommand(
                LocalDate.now(),
                UUID.randomUUID(),
                "Notas",
                List.of(lineCmd)
        );

        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);

        Quotation quotation = Quotation.builder()
                .id(quotationId)
                .clinicId(clinicId)
                .patientId(patientId)
                .status(QuotationStatus.ACCEPTED)
                .items(List.of())
                .build();

        when(quotationRepository.findByIdAndPatientIdAndClinicId(quotationId, patientId, clinicId))
                .thenReturn(Optional.of(quotation));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            visitService.registerVisit(actingUserId, patientId, quotationId, clinicId, command);
        });

        assertTrue(exception.getMessage().contains("no pertenece a esta cotización."));
        verifyNoInteractions(inventoryMaterialPort, visitRepository);
    }

    @Test
    void getVisitsByQuotation_Success() {
        // Arrange
        Visit visit = new Visit(
                UUID.randomUUID(), clinicId, patientId, quotationId,
                LocalDate.now(), UUID.randomUUID(), "Notas", List.of(),
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(visitRepository.findByQuotationIdAndPatientIdAndClinicId(quotationId, patientId, clinicId))
                .thenReturn(List.of(visit));

        // Act
        List<Visit> result = visitService.getVisitsByQuotation(actingUserId, quotationId, patientId, clinicId);

        // Assert
        assertEquals(1, result.size());
        assertEquals(visit.getId(), result.get(0).getId());
    }

    @Test
    void getVisitDetails_Success() {
        // Arrange
        UUID visitId = UUID.randomUUID();
        Visit visit = new Visit(
                visitId, clinicId, patientId, quotationId,
                LocalDate.now(), UUID.randomUUID(), "Notas", List.of(),
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(visitRepository.findByIdAndPatientIdAndClinicId(visitId, patientId, clinicId))
                .thenReturn(Optional.of(visit));

        // Act
        Visit result = visitService.getVisitDetails(actingUserId, visitId, patientId, clinicId);

        // Assert
        assertNotNull(result);
        assertEquals(visitId, result.getId());
    }

    @Test
    void getVisitDetails_NotFound_ThrowsException() {
        // Arrange
        UUID visitId = UUID.randomUUID();

        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(visitRepository.findByIdAndPatientIdAndClinicId(visitId, patientId, clinicId))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            visitService.getVisitDetails(actingUserId, visitId, patientId, clinicId);
        });

        assertEquals("La visita no existe.", exception.getMessage());
    }
}
