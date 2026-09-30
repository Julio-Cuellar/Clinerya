package com.jclinical.treatments.domain.service;

import com.jclinical.treatments.domain.model.PricingType;
import com.jclinical.treatments.domain.model.TreatmentCatalogItem;
import com.jclinical.treatments.domain.model.TreatmentCatalogMaterial;
import com.jclinical.treatments.domain.model.TreatmentCatalogSeeds;
import com.jclinical.treatments.domain.ports.out.ClinicSpecialtyPort;
import com.jclinical.treatments.domain.ports.in.ManageTreatmentCatalogUseCase;
import com.jclinical.treatments.domain.ports.in.PublicTreatmentCatalogUseCase;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort.MaterialSnapshot;
import com.jclinical.treatments.domain.ports.out.TreatmentCatalogRepositoryPort;
import com.jclinical.core.domain.ClinicSpecialty;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TreatmentCatalogService implements ManageTreatmentCatalogUseCase, PublicTreatmentCatalogUseCase {

    /** La descripcion que usa el asistente: breve, para el paciente. */
    public static final int MIN_ASSISTANT_DESCRIPTION = 20;
    public static final int MAX_ASSISTANT_DESCRIPTION = 300;
    /** Una jornada: ningun servicio dura mas. */
    public static final int MAX_DURATION_MINUTES = 600;

    private final TreatmentCatalogRepositoryPort catalogRepository;
    private final InventoryMaterialPort inventoryMaterialPort;
    private final StaffPermissionCheckerPort permissionChecker;
    private final ClinicSpecialtyPort clinicSpecialtyPort;

    public TreatmentCatalogService(TreatmentCatalogRepositoryPort catalogRepository,
                                   InventoryMaterialPort inventoryMaterialPort,
                                   StaffPermissionCheckerPort permissionChecker,
                                   ClinicSpecialtyPort clinicSpecialtyPort) {
        this.catalogRepository = catalogRepository;
        this.inventoryMaterialPort = inventoryMaterialPort;
        this.permissionChecker = permissionChecker;
        this.clinicSpecialtyPort = clinicSpecialtyPort;
    }

    @Override
    public SeedResult seedCatalogForSpecialty(UUID actingUserId, UUID clinicId) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_TREATMENT_CATALOG,
                "No tienes permiso para gestionar el catalogo de tratamientos de esta clinica.");

        ClinicSpecialty specialty = clinicSpecialtyPort.findByClinicId(clinicId)
                .orElse(ClinicSpecialty.SIN_CONFIGURAR);
        List<TreatmentCatalogSeeds.SeedItem> seeds = TreatmentCatalogSeeds.forSpecialty(specialty);
        if (seeds.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay un catálogo sugerido para la especialidad de esta clínica.");
        }

        // Clave natural: el nombre del servicio dentro de la clínica. Se comparan también los
        // inactivos, porque volver a crear algo que el médico dio de baja seria peor que omitirlo.
        Set<String> existing = catalogRepository.findByClinicId(clinicId, true).stream()
                .map(item -> normalize(item.getName()))
                .collect(Collectors.toSet());

        List<TreatmentCatalogItem> created = new ArrayList<>();
        int skipped = 0;
        for (TreatmentCatalogSeeds.SeedItem seed : seeds) {
            if (!existing.add(normalize(seed.name()))) {
                skipped++;
                continue;
            }
            created.add(catalogRepository.save(TreatmentCatalogItem.builder()
                    .id(UUID.randomUUID())
                    .clinicId(clinicId)
                    .name(seed.name())
                    .category(seed.category())
                    .defaultPrice(seed.defaultPrice())
                    .estimatedDurationMinutes(seed.estimatedDurationMinutes())
                    .materials(List.of())
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build()));
        }

        return new SeedResult(created.size(), skipped, created);
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public TreatmentCatalogItem createCatalogItem(UUID actingUserId, UUID clinicId, CreateCatalogItemCommand command) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_TREATMENT_CATALOG,
                "No tienes permiso para gestionar el catalogo de tratamientos de esta clinica.");
        validate(command.name(), command.pricingType(), command.defaultPrice(), command.estimatedDurationMinutes(),
                command.availableInAssistant(), command.description());

        TreatmentCatalogItem item = TreatmentCatalogItem.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .name(command.name())
                .category(command.category())
                .description(command.description())
                .defaultPrice(command.defaultPrice())
                .estimatedDurationMinutes(command.estimatedDurationMinutes())
                .pricingType(command.pricingType())
                .availableInAssistant(command.availableInAssistant())
                .materials(toMaterials(command.materials(), clinicId))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return catalogRepository.save(item);
    }

    @Override
    public TreatmentCatalogItem updateCatalogItem(UUID actingUserId, UUID itemId, UUID clinicId, UpdateCatalogItemCommand command) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_TREATMENT_CATALOG,
                "No tienes permiso para gestionar el catalogo de tratamientos de esta clinica.");
        validate(command.name(), command.pricingType(), command.defaultPrice(), command.estimatedDurationMinutes(),
                command.availableInAssistant(), command.description());

        TreatmentCatalogItem item = catalogRepository.findByIdAndClinicId(itemId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El servicio no existe en esta clínica."));

        item.setName(command.name());
        item.setCategory(command.category());
        item.setDescription(command.description());
        item.setDefaultPrice(command.defaultPrice());
        item.setEstimatedDurationMinutes(command.estimatedDurationMinutes());
        item.setPricingType(command.pricingType());
        item.setAvailableInAssistant(command.availableInAssistant());
        item.setMaterials(toMaterials(command.materials(), clinicId));
        item.setActive(command.active());
        item.setUpdatedAt(LocalDateTime.now());

        return catalogRepository.save(item);
    }

    @Override
    public void deactivateCatalogItem(UUID actingUserId, UUID itemId, UUID clinicId) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_TREATMENT_CATALOG,
                "No tienes permiso para gestionar el catalogo de tratamientos de esta clinica.");
        TreatmentCatalogItem item = catalogRepository.findByIdAndClinicId(itemId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El servicio no existe en esta clínica."));
        item.setActive(false);
        item.setUpdatedAt(LocalDateTime.now());
        catalogRepository.save(item);
    }

    @Override
    public Optional<TreatmentCatalogItem> getCatalogItem(UUID actingUserId, UUID itemId, UUID clinicId) {
        requirePermission(clinicId, actingUserId, StaffPermission.VIEW_TREATMENTS,
                "No tienes permiso para consultar el catalogo de tratamientos de esta clinica.");
        return catalogRepository.findByIdAndClinicId(itemId, clinicId);
    }

    @Override
    public List<TreatmentCatalogItem> getCatalogItemsByClinic(UUID actingUserId, UUID clinicId, boolean includeInactive) {
        requirePermission(clinicId, actingUserId, StaffPermission.VIEW_TREATMENTS,
                "No tienes permiso para consultar el catalogo de tratamientos de esta clinica.");
        return catalogRepository.findByClinicId(clinicId, includeInactive);
    }

    @Override
    public List<PublicTreatment> activeTreatments(UUID clinicId) {
        return catalogRepository.findByClinicId(clinicId, false).stream()
                .filter(TreatmentCatalogItem::isActive)
                .sorted(java.util.Comparator.comparing(item -> item.getName().toLowerCase(Locale.ROOT)))
                .map(item -> new PublicTreatment(item.getName(), item.getCategory(), item.getDefaultPrice()))
                .toList();
    }

    @Override
    public List<PublicService> assistantServices(UUID clinicId) {
        return catalogRepository.findByClinicId(clinicId, false).stream()
                .filter(item -> item.isActive() && item.isAvailableInAssistant() && readyForAssistant(item))
                .sorted(java.util.Comparator.comparing(item -> item.getName().toLowerCase(Locale.ROOT)))
                .map(TreatmentCatalogService::toPublicService)
                .toList();
    }

    @Override
    public Optional<PublicService> activeService(UUID clinicId, UUID serviceId) {
        if (clinicId == null || serviceId == null) {
            return Optional.empty();
        }
        return catalogRepository.findByIdAndClinicId(serviceId, clinicId)
                .filter(TreatmentCatalogItem::isActive)
                .map(TreatmentCatalogService::toPublicService);
    }

    /** Completo para el asistente: descripcion breve valida y duracion. */
    private static boolean readyForAssistant(TreatmentCatalogItem item) {
        String description = item.getDescription() == null ? "" : item.getDescription().strip();
        return item.getEstimatedDurationMinutes() != null && item.getEstimatedDurationMinutes() > 0
                && description.length() >= MIN_ASSISTANT_DESCRIPTION && description.length() <= MAX_ASSISTANT_DESCRIPTION;
    }

    private static PublicService toPublicService(TreatmentCatalogItem item) {
        return new PublicService(item.getId(), item.getName(), item.getCategory(),
                item.getDescription() == null ? null : item.getDescription().strip(),
                item.getPricingType() == null ? PricingType.FIXED : item.getPricingType(), item.getDefaultPrice(),
                item.getEstimatedDurationMinutes());
    }

    private void requirePermission(UUID clinicId, UUID actingUserId, StaffPermission permission, String deniedMessage) {
        if (actingUserId == null) {
            throw new ClinicAccessDeniedException("Usuario no autenticado.");
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, permission)) {
            throw new ClinicAccessDeniedException(deniedMessage);
        }
    }

    private List<TreatmentCatalogMaterial> toMaterials(List<CatalogMaterialCommand> commands, UUID clinicId) {
        if (commands == null) {
            return List.of();
        }
        return commands.stream().map(command -> toMaterial(command, clinicId)).toList();
    }

    private TreatmentCatalogMaterial toMaterial(CatalogMaterialCommand command, UUID clinicId) {
        if (command.materialId() == null) {
            throw new IllegalArgumentException("El material del checklist es obligatorio.");
        }
        MaterialSnapshot snapshot = inventoryMaterialPort.findActiveMaterial(command.materialId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El material no existe o no está activo en esta clínica."));
        if (command.typicalQuantity() != null && command.typicalQuantity().signum() <= 0) {
            throw new IllegalArgumentException("La cantidad típica debe ser mayor a cero si se especifica.");
        }
        return TreatmentCatalogMaterial.builder()
                .id(UUID.randomUUID())
                .materialId(snapshot.materialId())
                .materialName(snapshot.name())
                .typicalQuantity(command.typicalQuantity())
                .build();
    }

    /**
     * Reglas del servicio: nombre, tipo de precio, precio (obligatorio si es fijo), duracion y, si el
     * asistente lo ofrece, una descripcion breve para el paciente.
     */
    private void validate(String name, PricingType pricingType, BigDecimal defaultPrice, Integer durationMinutes,
                          boolean availableInAssistant, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del servicio es obligatorio.");
        }
        if (pricingType == null) {
            throw new IllegalArgumentException("Elige si el servicio tiene precio fijo o varía por paciente.");
        }
        if (pricingType == PricingType.FIXED && defaultPrice == null) {
            throw new IllegalArgumentException("Un servicio de precio fijo necesita su precio.");
        }
        if (defaultPrice != null && defaultPrice.signum() < 0) {
            throw new IllegalArgumentException("El precio debe ser mayor o igual a cero.");
        }
        if (durationMinutes == null || durationMinutes <= 0 || durationMinutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException("La duración del servicio es obligatoria (entre 1 y " + MAX_DURATION_MINUTES
                    + " minutos).");
        }
        if (availableInAssistant) {
            String text = description == null ? "" : description.strip();
            if (text.length() < MIN_ASSISTANT_DESCRIPTION || text.length() > MAX_ASSISTANT_DESCRIPTION) {
                throw new IllegalArgumentException("Para ofrecerlo por el asistente escribe una descripción para el paciente de "
                        + MIN_ASSISTANT_DESCRIPTION + " a " + MAX_ASSISTANT_DESCRIPTION + " caracteres.");
            }
        }
    }
}
