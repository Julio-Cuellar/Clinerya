package com.jclinical.treatments.infra.adapters.in.web;

import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.model.QuotationItem;
import com.jclinical.treatments.domain.model.QuotationItemMaterial;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase.CreateQuotationCommand;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase.MaterialLineCommand;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase.QuotationItemCommand;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase.ReplaceItemsCommand;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase.UpdateHeaderCommand;
import com.jclinical.treatments.infra.adapters.in.web.dto.CreateQuotationRequest;
import com.jclinical.treatments.infra.adapters.in.web.dto.MaterialLineRequest;
import com.jclinical.treatments.infra.adapters.in.web.dto.MaterialLineResponse;
import com.jclinical.treatments.infra.adapters.in.web.dto.QuotationItemRequest;
import com.jclinical.treatments.infra.adapters.in.web.dto.QuotationItemResponse;
import com.jclinical.treatments.infra.adapters.in.web.dto.QuotationResponse;
import com.jclinical.treatments.infra.adapters.in.web.dto.ReplaceQuotationItemsRequest;
import com.jclinical.treatments.infra.adapters.in.web.dto.TransitionQuotationStatusRequest;
import com.jclinical.treatments.infra.adapters.in.web.dto.UpdateItemProgressRequest;
import com.jclinical.treatments.infra.adapters.in.web.dto.UpdateQuotationHeaderRequest;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/patients/{patientId}/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final ManageQuotationUseCase quotationUseCase;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<QuotationResponse> createQuotation(
            @PathVariable UUID patientId,
            @RequestBody CreateQuotationRequest request) {
        CreateQuotationCommand command = new CreateQuotationCommand(
                request.createdByUserId(),
                request.quotationDate(),
                request.notes(),
                request.validUntil(),
                toItemCommands(request.items())
        );
        Quotation quotation = quotationUseCase.createQuotation(currentUserResolver.getCurrentUserId(), patientId, request.clinicId(), command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(quotation));
    }

    @GetMapping
    public ResponseEntity<List<QuotationResponse>> getQuotations(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId) {
        List<QuotationResponse> responses = quotationUseCase.getQuotationsByPatient(currentUserResolver.getCurrentUserId(), patientId, clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{quotationId}")
    public ResponseEntity<QuotationResponse> getQuotation(
            @PathVariable UUID patientId,
            @PathVariable UUID quotationId,
            @RequestParam UUID clinicId) {
        return quotationUseCase.getQuotation(quotationId, patientId, clinicId)
                .map(quotation -> ResponseEntity.ok(toResponse(quotation)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{quotationId}")
    public ResponseEntity<QuotationResponse> updateHeader(
            @PathVariable UUID patientId,
            @PathVariable UUID quotationId,
            @RequestBody UpdateQuotationHeaderRequest request) {
        UpdateHeaderCommand command = new UpdateHeaderCommand(
                request.notes(),
                request.validUntil(),
                request.quotationDate()
        );
        Quotation quotation = quotationUseCase.updateQuotationHeader(currentUserResolver.getCurrentUserId(), quotationId, patientId, request.clinicId(), command);
        return ResponseEntity.ok(toResponse(quotation));
    }

    @PutMapping("/{quotationId}/items")
    public ResponseEntity<QuotationResponse> replaceItems(
            @PathVariable UUID patientId,
            @PathVariable UUID quotationId,
            @RequestBody ReplaceQuotationItemsRequest request) {
        ReplaceItemsCommand command = new ReplaceItemsCommand(toItemCommands(request.items()));
        Quotation quotation = quotationUseCase.replaceQuotationItems(currentUserResolver.getCurrentUserId(), quotationId, patientId, request.clinicId(), command);
        return ResponseEntity.ok(toResponse(quotation));
    }

    @PatchMapping("/{quotationId}/status")
    public ResponseEntity<QuotationResponse> transitionStatus(
            @PathVariable UUID patientId,
            @PathVariable UUID quotationId,
            @RequestBody TransitionQuotationStatusRequest request) {
        Quotation quotation = quotationUseCase.transitionStatus(currentUserResolver.getCurrentUserId(), quotationId, patientId, request.clinicId(), request.targetStatus());
        return ResponseEntity.ok(toResponse(quotation));
    }

    @PatchMapping("/{quotationId}/items/{itemId}/progress")
    public ResponseEntity<QuotationResponse> updateItemProgress(
            @PathVariable UUID patientId,
            @PathVariable UUID quotationId,
            @PathVariable UUID itemId,
            @RequestBody UpdateItemProgressRequest request) {
        Quotation quotation = quotationUseCase.updateItemProgress(currentUserResolver.getCurrentUserId(), quotationId, itemId, patientId, request.clinicId(), request.progressStatus());
        return ResponseEntity.ok(toResponse(quotation));
    }

    @DeleteMapping("/{quotationId}")
    public ResponseEntity<Void> deleteQuotation(
            @PathVariable UUID patientId,
            @PathVariable UUID quotationId,
            @RequestParam UUID clinicId) {
        quotationUseCase.deleteQuotation(currentUserResolver.getCurrentUserId(), quotationId, patientId, clinicId);
        return ResponseEntity.noContent().build();
    }

    private List<QuotationItemCommand> toItemCommands(List<QuotationItemRequest> items) {
        return items.stream()
                .map(item -> new QuotationItemCommand(
                        item.catalogItemId(),
                        item.description(),
                        item.toothNumber(),
                        item.laborCharge(),
                        toMaterialLineCommands(item.materials()),
                        item.discountPercentage()
                ))
                .toList();
    }

    private List<MaterialLineCommand> toMaterialLineCommands(List<MaterialLineRequest> materials) {
        if (materials == null) {
            return List.of();
        }
        return materials.stream()
                .map(material -> new MaterialLineCommand(
                        material.materialId(),
                        material.materialName(),
                        material.estimatedQuantity(),
                        material.manualUnitCost()
                ))
                .toList();
    }

    private QuotationResponse toResponse(Quotation quotation) {
        List<QuotationItemResponse> items = quotation.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new QuotationResponse(
                quotation.getId(),
                quotation.getClinicId(),
                quotation.getPatientId(),
                quotation.getCreatedByUserId(),
                quotation.getQuotationDate(),
                quotation.getStatus(),
                quotation.getNotes(),
                quotation.getValidUntil(),
                items,
                quotation.grandTotal(),
                quotation.getCreatedAt(),
                quotation.getUpdatedAt()
        );
    }

    private QuotationItemResponse toItemResponse(QuotationItem item) {
        List<MaterialLineResponse> materials = item.getMaterials() == null
                ? List.of()
                : item.getMaterials().stream().map(this::toMaterialResponse).toList();
        return new QuotationItemResponse(
                item.getId(),
                item.getCatalogItemId(),
                item.getDescription(),
                item.getToothNumber(),
                item.getLaborCharge(),
                materials,
                item.materialsTotal(),
                item.getDiscountPercentage(),
                item.getProgressStatus(),
                item.subtotal()
        );
    }

    private MaterialLineResponse toMaterialResponse(QuotationItemMaterial material) {
        return new MaterialLineResponse(
                material.getId(),
                material.getMaterialId(),
                material.getMaterialName(),
                material.getEstimatedQuantity(),
                material.getUnitCostAtQuote(),
                material.estimatedCost()
        );
    }
}
