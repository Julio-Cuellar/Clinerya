package com.jclinical.treatments.domain.service;

import com.jclinical.core.events.ConsumoConciliadoEvent;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import com.jclinical.treatments.domain.model.Visit;
import com.jclinical.treatments.domain.model.VisitLineItem;
import com.jclinical.treatments.domain.model.VisitMaterialUsage;
import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.model.QuotationStatus;
import com.jclinical.treatments.domain.ports.in.ManageVisitsUseCase;
import com.jclinical.treatments.domain.ports.out.VisitRepositoryPort;
import com.jclinical.treatments.domain.ports.out.QuotationRepositoryPort;
import com.jclinical.treatments.domain.ports.out.PatientValidatorPort;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort.MaterialSnapshot;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VisitService implements ManageVisitsUseCase {

    private final VisitRepositoryPort visitRepository;
    private final QuotationRepositoryPort quotationRepository;
    private final PatientValidatorPort patientValidator;
    private final InventoryMaterialPort inventoryMaterialPort;
    private final DomainEventPublisherPort eventPublisher;
    private final StaffPermissionCheckerPort permissionChecker;

    public VisitService(
            VisitRepositoryPort visitRepository,
            QuotationRepositoryPort quotationRepository,
            PatientValidatorPort patientValidator,
            InventoryMaterialPort inventoryMaterialPort,
            DomainEventPublisherPort eventPublisher,
            StaffPermissionCheckerPort permissionChecker) {
        this.visitRepository = visitRepository;
        this.quotationRepository = quotationRepository;
        this.patientValidator = patientValidator;
        this.inventoryMaterialPort = inventoryMaterialPort;
        this.eventPublisher = eventPublisher;
        this.permissionChecker = permissionChecker;
    }

    @Override
    public Visit registerVisit(UUID actingUserId, UUID patientId, UUID quotationId, UUID clinicId, RegisterVisitCommand command) {
        requirePermission(clinicId, actingUserId, StaffPermission.CREATE_VISITS,
                "No tienes permiso para registrar sesiones clinicas en esta clinica.");
        validatePatient(patientId, clinicId);

        Quotation quotation = null;
        if (quotationId != null) {
            quotation = quotationRepository.findByIdAndPatientIdAndClinicId(quotationId, patientId, clinicId)
                    .orElseThrow(() -> new IllegalArgumentException("La cotización no existe para este paciente en esta clínica."));

            if (quotation.getStatus() != QuotationStatus.ACCEPTED) {
                throw new IllegalStateException("Solo se pueden registrar sesiones clínicas para cotizaciones aceptadas.");
            }
        }

        UUID visitId = UUID.randomUUID();
        List<VisitLineItem> visitItems = new ArrayList<>();
        List<ConsumoConciliadoEvent.MaterialLine> eventLines = new ArrayList<>();
        Map<UUID, AggregatedMaterialUsage> inventoryUsages = new LinkedHashMap<>();
        UUID eventQuotationItemId = null;

        for (RegisterVisitLineItemCommand itemCmd : command.items()) {
            if (quotation != null && itemCmd.quotationItemId() != null) {
                boolean itemExists = quotation.getItems().stream()
                        .anyMatch(qItem -> qItem.getId().equals(itemCmd.quotationItemId()));
                if (!itemExists) {
                    throw new IllegalArgumentException("La partida de cotización con ID " + itemCmd.quotationItemId() + " no pertenece a esta cotización.");
                }
            }
            if (eventQuotationItemId == null) {
                eventQuotationItemId = itemCmd.quotationItemId();
            }

            List<VisitMaterialUsage> usages = new ArrayList<>();
            for (RegisterVisitMaterialUsageCommand usageCmd : itemCmd.materialsUsed()) {
                UUID usageId = UUID.randomUUID();

                if (usageCmd.materialId() != null) {
                    if (usageCmd.actualQuantity() == null || usageCmd.actualQuantity().signum() <= 0) {
                        throw new IllegalArgumentException("La cantidad usada de '" + usageCmd.materialName() + "' debe ser mayor a cero.");
                    }
                    inventoryUsages.merge(
                            usageCmd.materialId(),
                            new AggregatedMaterialUsage(
                                    usageCmd.materialId(),
                                    usageCmd.materialName(),
                                    usageCmd.actualQuantity()
                            ),
                            AggregatedMaterialUsage::add
                    );
                }

                usages.add(new VisitMaterialUsage(
                        usageId,
                        usageCmd.materialId(),
                        usageCmd.materialName(),
                        usageCmd.actualQuantity()
                ));
            }

            visitItems.add(new VisitLineItem(
                    UUID.randomUUID(),
                    itemCmd.quotationItemId(),
                    usages
            ));
        }

        Visit visit = new Visit(
                visitId,
                clinicId,
                patientId,
                quotationId,
                command.visitDate() != null ? command.visitDate() : java.time.LocalDate.now(),
                command.doctorId(),
                command.notes(),
                visitItems,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        for (AggregatedMaterialUsage usage : inventoryUsages.values()) {
            MaterialSnapshot material = inventoryMaterialPort.findActiveMaterial(usage.materialId(), clinicId)
                    .orElseThrow(() -> new IllegalArgumentException("El material '" + usage.materialName() + "' no existe o está inactivo en esta clínica."));

            inventoryMaterialPort.registerUsage(
                    clinicId,
                    usage.materialId(),
                    usage.quantity(),
                    "VISIT",
                    visitId,
                    command.notes()
            );

            eventLines.add(new ConsumoConciliadoEvent.MaterialLine(
                    usage.materialId(),
                    usage.materialName(),
                    usage.quantity(),
                    material.unitCost()
            ));
        }

        Visit saved = visitRepository.save(visit);

        if (!eventLines.isEmpty()) {
            eventPublisher.publish(DomainEventRoutingKeys.CONSUMPTION_RECONCILED, new ConsumoConciliadoEvent(
                    UUID.randomUUID(),
                    clinicId,
                    patientId,
                    visitId,
                    quotationId,
                    eventQuotationItemId,
                    LocalDateTime.now(),
                    eventLines
            ));
        }

        return saved;
    }

    @Override
    public List<Visit> getVisitsByQuotation(UUID actingUserId, UUID quotationId, UUID patientId, UUID clinicId) {
        requirePermission(clinicId, actingUserId, StaffPermission.VIEW_PATIENT_CARE,
                "No tienes permiso para consultar la atencion clinica de esta clinica.");
        validatePatient(patientId, clinicId);
        return visitRepository.findByQuotationIdAndPatientIdAndClinicId(quotationId, patientId, clinicId);
    }

    @Override
    public Visit getVisitDetails(UUID actingUserId, UUID visitId, UUID patientId, UUID clinicId) {
        requirePermission(clinicId, actingUserId, StaffPermission.VIEW_PATIENT_CARE,
                "No tienes permiso para consultar la atencion clinica de esta clinica.");
        validatePatient(patientId, clinicId);
        return visitRepository.findByIdAndPatientIdAndClinicId(visitId, patientId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La visita no existe."));
    }

    private void requirePermission(UUID clinicId, UUID actingUserId, StaffPermission permission, String deniedMessage) {
        if (actingUserId == null) {
            throw new ClinicAccessDeniedException("Usuario no autenticado.");
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, permission)) {
            throw new ClinicAccessDeniedException(deniedMessage);
        }
    }

    private void validatePatient(UUID patientId, UUID clinicId) {
        if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clínica.");
        }
    }

    private record AggregatedMaterialUsage(UUID materialId, String materialName, BigDecimal quantity) {
        private AggregatedMaterialUsage add(AggregatedMaterialUsage other) {
            return new AggregatedMaterialUsage(materialId, materialName, quantity.add(other.quantity()));
        }
    }
}
