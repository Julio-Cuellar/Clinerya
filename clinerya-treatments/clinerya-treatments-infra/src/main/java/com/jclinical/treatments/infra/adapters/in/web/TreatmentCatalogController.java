package com.jclinical.treatments.infra.adapters.in.web;

import com.jclinical.treatments.domain.model.TreatmentCatalogItem;
import com.jclinical.treatments.domain.model.TreatmentCatalogMaterial;
import com.jclinical.treatments.domain.ports.in.ManageTreatmentCatalogUseCase;
import com.jclinical.treatments.domain.ports.in.ManageTreatmentCatalogUseCase.CatalogMaterialCommand;
import com.jclinical.treatments.domain.ports.in.ManageTreatmentCatalogUseCase.CreateCatalogItemCommand;
import com.jclinical.treatments.domain.ports.in.ManageTreatmentCatalogUseCase.UpdateCatalogItemCommand;
import com.jclinical.treatments.infra.adapters.in.web.dto.CatalogMaterialRequest;
import com.jclinical.treatments.infra.adapters.in.web.dto.CatalogMaterialResponse;
import com.jclinical.treatments.infra.adapters.in.web.dto.CreateTreatmentCatalogItemRequest;
import com.jclinical.treatments.infra.adapters.in.web.dto.TreatmentCatalogItemResponse;
import com.jclinical.treatments.infra.adapters.in.web.dto.UpdateTreatmentCatalogItemRequest;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/treatment-catalog")
@RequiredArgsConstructor
public class TreatmentCatalogController {

    private final ManageTreatmentCatalogUseCase catalogUseCase;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<TreatmentCatalogItemResponse> createItem(
            @PathVariable UUID clinicId,
            @RequestBody CreateTreatmentCatalogItemRequest request) {
        CreateCatalogItemCommand command = new CreateCatalogItemCommand(
                request.name(),
                request.category(),
                request.description(),
                request.defaultPrice(),
                request.estimatedDurationMinutes(),
                toMaterialCommands(request.materials())
        );
        TreatmentCatalogItem item = catalogUseCase.createCatalogItem(clinicId, currentUserResolver.getCurrentUserId(), command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(item));
    }

    @GetMapping
    public ResponseEntity<List<TreatmentCatalogItemResponse>> getItems(
            @PathVariable UUID clinicId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<TreatmentCatalogItemResponse> responses = catalogUseCase.getCatalogItemsByClinic(clinicId, currentUserResolver.getCurrentUserId(), includeInactive).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<TreatmentCatalogItemResponse> getItem(
            @PathVariable UUID clinicId,
            @PathVariable UUID itemId) {
        return catalogUseCase.getCatalogItem(itemId, clinicId, currentUserResolver.getCurrentUserId())
                .map(item -> ResponseEntity.ok(toResponse(item)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<TreatmentCatalogItemResponse> updateItem(
            @PathVariable UUID clinicId,
            @PathVariable UUID itemId,
            @RequestBody UpdateTreatmentCatalogItemRequest request) {
        UpdateCatalogItemCommand command = new UpdateCatalogItemCommand(
                request.name(),
                request.category(),
                request.description(),
                request.defaultPrice(),
                request.estimatedDurationMinutes(),
                toMaterialCommands(request.materials()),
                request.active()
        );
        TreatmentCatalogItem item = catalogUseCase.updateCatalogItem(itemId, clinicId, currentUserResolver.getCurrentUserId(), command);
        return ResponseEntity.ok(toResponse(item));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deactivateItem(
            @PathVariable UUID clinicId,
            @PathVariable UUID itemId) {
        catalogUseCase.deactivateCatalogItem(itemId, clinicId, currentUserResolver.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    private TreatmentCatalogItemResponse toResponse(TreatmentCatalogItem item) {
        List<CatalogMaterialResponse> materials = item.getMaterials() == null
                ? List.of()
                : item.getMaterials().stream().map(this::toMaterialResponse).toList();
        return new TreatmentCatalogItemResponse(
                item.getId(),
                item.getClinicId(),
                item.getName(),
                item.getCategory(),
                item.getDescription(),
                item.getDefaultPrice(),
                item.getEstimatedDurationMinutes(),
                materials,
                item.isActive(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private CatalogMaterialResponse toMaterialResponse(TreatmentCatalogMaterial material) {
        return new CatalogMaterialResponse(
                material.getId(),
                material.getMaterialId(),
                material.getMaterialName(),
                material.getTypicalQuantity()
        );
    }

    private List<CatalogMaterialCommand> toMaterialCommands(List<CatalogMaterialRequest> materials) {
        if (materials == null) {
            return List.of();
        }
        return materials.stream()
                .map(material -> new CatalogMaterialCommand(material.materialId(), material.typicalQuantity()))
                .toList();
    }
}
