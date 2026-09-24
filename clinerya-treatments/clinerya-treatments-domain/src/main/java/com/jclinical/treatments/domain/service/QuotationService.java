package com.jclinical.treatments.domain.service;

import com.jclinical.core.domain.ClinicSpecialty;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.treatments.domain.model.ItemProgressStatus;
import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.model.QuotationItem;
import com.jclinical.treatments.domain.model.QuotationItemMaterial;
import com.jclinical.treatments.domain.model.QuotationStatus;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase;
import com.jclinical.treatments.domain.ports.in.QuotationLookupUseCase;
import com.jclinical.treatments.domain.ports.out.ClinicSpecialtyPort;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort.MaterialSnapshot;
import com.jclinical.treatments.domain.ports.out.PatientValidatorPort;
import com.jclinical.treatments.domain.ports.out.QuotationRepositoryPort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class QuotationService implements ManageQuotationUseCase, QuotationLookupUseCase {

    private final QuotationRepositoryPort quotationRepository;
    private final PatientValidatorPort patientValidator;
    private final InventoryMaterialPort inventoryMaterialPort;
    private final PatientAccessAuthorizationPort accessAuthorizationPort;
    private final ClinicSpecialtyPort clinicSpecialtyPort;

    public QuotationService(
            QuotationRepositoryPort quotationRepository,
            PatientValidatorPort patientValidator,
            InventoryMaterialPort inventoryMaterialPort,
            PatientAccessAuthorizationPort accessAuthorizationPort,
            ClinicSpecialtyPort clinicSpecialtyPort) {
        this.quotationRepository = quotationRepository;
        this.patientValidator = patientValidator;
        this.inventoryMaterialPort = inventoryMaterialPort;
        this.accessAuthorizationPort = accessAuthorizationPort;
        this.clinicSpecialtyPort = clinicSpecialtyPort;
    }

    @Override
    public Quotation createQuotation(UUID patientId, UUID clinicId, UUID actingUserId, CreateQuotationCommand command) {
        authorize(actingUserId, patientId, clinicId, true);

        Quotation quotation = Quotation.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .patientId(patientId)
                .createdByUserId(actingUserId)
                .quotationDate(command.quotationDate() != null ? command.quotationDate() : LocalDate.now())
                .status(QuotationStatus.DRAFT)
                .notes(command.notes())
                .validUntil(command.validUntil())
                .items(toItems(command.items(), clinicId))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return quotationRepository.save(quotation);
    }

    @Override
    public Quotation updateQuotationHeader(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId, UpdateHeaderCommand command) {
        authorize(actingUserId, patientId, clinicId, true);
        Quotation quotation = findOrThrow(quotationId, patientId, clinicId);
        quotation.updateHeader(command.notes(), command.validUntil(), command.quotationDate());
        return quotationRepository.save(quotation);
    }

    @Override
    public Quotation replaceQuotationItems(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId, ReplaceItemsCommand command) {
        authorize(actingUserId, patientId, clinicId, true);
        Quotation quotation = findOrThrow(quotationId, patientId, clinicId);
        quotation.replaceItems(toItems(command.items(), clinicId));
        return quotationRepository.save(quotation);
    }

    @Override
    public Quotation transitionStatus(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId, QuotationStatus targetStatus) {
        authorize(actingUserId, patientId, clinicId, true);
        Quotation quotation = findOrThrow(quotationId, patientId, clinicId);

        switch (targetStatus) {
            case SENT -> quotation.send();
            case ACCEPTED -> quotation.accept();
            case REJECTED -> quotation.reject();
            case EXPIRED -> quotation.expire();
            default -> throw new IllegalArgumentException("Transición de estado no soportada: " + targetStatus);
        }

        return quotationRepository.save(quotation);
    }

    @Override
    public Quotation updateItemProgress(UUID quotationId, UUID itemId, UUID patientId, UUID clinicId, UUID actingUserId, ItemProgressStatus targetStatus) {
        authorize(actingUserId, patientId, clinicId, true);
        Quotation quotation = findOrThrow(quotationId, patientId, clinicId);
        quotation.updateItemProgress(itemId, targetStatus);
        return quotationRepository.save(quotation);
    }

    @Override
    public Optional<Quotation> getQuotation(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId) {
        authorize(actingUserId, patientId, clinicId, false);
        return quotationRepository.findByIdAndPatientIdAndClinicId(quotationId, patientId, clinicId);
    }

    @Override
    public List<Quotation> getQuotationsByPatient(UUID patientId, UUID clinicId, UUID actingUserId) {
        authorize(actingUserId, patientId, clinicId, false);
        return quotationRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId);
    }

    @Override
    public void deleteQuotation(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId) {
        authorize(actingUserId, patientId, clinicId, true);
        Quotation quotation = findOrThrow(quotationId, patientId, clinicId);
        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new IllegalStateException("Solo una cotización en borrador puede eliminarse.");
        }
        quotationRepository.deleteById(quotationId);
    }

    /**
     * Lectura interna entre modulos (agenda, caja): no hay usuario en la peticion, asi que
     * no se autoriza contra el expediente. No exponer desde un controlador.
     */
    @Override
    public Optional<Quotation> findQuotationForSystem(UUID quotationId, UUID patientId, UUID clinicId) {
        return quotationRepository.findByIdAndPatientIdAndClinicId(quotationId, patientId, clinicId);
    }

    /**
     * Mismo patron que MedicalHistoryService: la autorizacion vive en el dominio, no en el
     * controlador, para que un endpoint nuevo no pueda saltarsela por descuido.
     */
    private void authorize(UUID actingUserId, UUID patientId, UUID clinicId, boolean requireWrite) {
        validatePatient(patientId, clinicId);
        if (actingUserId == null) {
            throw new ClinicAccessDeniedException("No tienes acceso a este expediente.");
        }
        AccessDecision decision = accessAuthorizationPort.resolveAccess(actingUserId, clinicId, patientId);
        if (decision.level() == AccessLevel.NONE || (requireWrite && decision.level() != AccessLevel.READ_WRITE)) {
            throw new ClinicAccessDeniedException("No tienes acceso a este expediente.");
        }
    }

    private Quotation findOrThrow(UUID quotationId, UUID patientId, UUID clinicId) {
        return quotationRepository.findByIdAndPatientIdAndClinicId(quotationId, patientId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La cotización no existe para este paciente en esta clínica."));
    }

    private void validatePatient(UUID patientId, UUID clinicId) {
        if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clínica.");
        }
    }

    private List<QuotationItem> toItems(List<QuotationItemCommand> commands, UUID clinicId) {
        // El perfil se resuelve una sola vez por cotizacion, no una por partida.
        ClinicSpecialty specialty = clinicSpecialtyPort.findByClinicId(clinicId)
                .orElse(ClinicSpecialty.SIN_CONFIGURAR);
        return commands.stream().map(command -> toItem(command, clinicId, specialty)).toList();
    }

    private QuotationItem toItem(QuotationItemCommand command, UUID clinicId, ClinicSpecialty specialty) {
        validateItem(command, specialty);
        return QuotationItem.builder()
                .id(UUID.randomUUID())
                .catalogItemId(command.catalogItemId())
                .description(command.description())
                .toothNumber(command.toothNumber())
                .laborCharge(command.laborCharge())
                .materials(toMaterials(command.materials(), clinicId))
                .discountPercentage(command.discountPercentage())
                .progressStatus(ItemProgressStatus.PENDING)
                .build();
    }

    private List<QuotationItemMaterial> toMaterials(List<MaterialLineCommand> commands, UUID clinicId) {
        if (commands == null) {
            return List.of();
        }
        return commands.stream().map(command -> toMaterial(command, clinicId)).toList();
    }

    private QuotationItemMaterial toMaterial(MaterialLineCommand command, UUID clinicId) {
        if (command.estimatedQuantity() == null || command.estimatedQuantity().signum() <= 0) {
            throw new IllegalArgumentException("La cantidad estimada del material debe ser mayor a cero.");
        }

        if (command.materialId() != null) {
            MaterialSnapshot snapshot = inventoryMaterialPort.findActiveMaterial(command.materialId(), clinicId)
                    .orElseThrow(() -> new IllegalArgumentException("El material no existe o no está activo en esta clínica."));
            return QuotationItemMaterial.builder()
                    .id(UUID.randomUUID())
                    .materialId(snapshot.materialId())
                    .materialName(snapshot.name())
                    .estimatedQuantity(command.estimatedQuantity())
                    .unitCostAtQuote(snapshot.unitCost())
                    .build();
        }

        if (command.materialName() == null || command.materialName().isBlank()) {
            throw new IllegalArgumentException("El material personalizado requiere un nombre.");
        }
        if (command.manualUnitCost() == null || command.manualUnitCost().signum() < 0) {
            throw new IllegalArgumentException("El material personalizado requiere un costo unitario válido.");
        }

        return QuotationItemMaterial.builder()
                .id(UUID.randomUUID())
                .materialId(null)
                .materialName(command.materialName())
                .estimatedQuantity(command.estimatedQuantity())
                .unitCostAtQuote(command.manualUnitCost())
                .build();
    }

    private void validateItem(QuotationItemCommand command, ClinicSpecialty specialty) {
        if (command.description() == null || command.description().isBlank()) {
            throw new IllegalArgumentException("La descripción de la partida es obligatoria.");
        }
        if (command.laborCharge() == null || command.laborCharge().signum() < 0) {
            throw new IllegalArgumentException("El cargo de mano de obra debe ser mayor o igual a cero.");
        }
        BigDecimal discount = command.discountPercentage();
        if (discount != null && (discount.signum() < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("El descuento debe estar entre 0 y 100.");
        }
        if (command.toothNumber() != null) {
            if (!specialty.usaLocalizadorDental()) {
                throw new IllegalArgumentException(
                        "Esta clínica no registra número de diente en sus partidas. "
                                + "Indica la zona en la descripción del concepto.");
            }
            if (!isValidFdiTooth(command.toothNumber())) {
                throw new IllegalArgumentException("El número de diente no es una notación FDI válida.");
            }
        }
    }

    private boolean isValidFdiTooth(int toothNumber) {
        int quadrant = toothNumber / 10;
        int position = toothNumber % 10;
        boolean permanent = quadrant >= 1 && quadrant <= 4 && position >= 1 && position <= 8;
        boolean deciduous = quadrant >= 5 && quadrant <= 8 && position >= 1 && position <= 5;
        return permanent || deciduous;
    }
}
