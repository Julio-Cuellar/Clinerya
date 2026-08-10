package com.jclinical.treatments.infra.adapters.in.web;

import com.jclinical.treatments.domain.model.Visit;
import com.jclinical.treatments.domain.model.VisitLineItem;
import com.jclinical.treatments.domain.model.VisitMaterialUsage;
import com.jclinical.treatments.domain.ports.in.ManageVisitsUseCase;
import com.jclinical.treatments.domain.ports.in.ManageVisitsUseCase.RegisterVisitCommand;
import com.jclinical.treatments.domain.ports.in.ManageVisitsUseCase.RegisterVisitLineItemCommand;
import com.jclinical.treatments.domain.ports.in.ManageVisitsUseCase.RegisterVisitMaterialUsageCommand;
import com.jclinical.treatments.infra.adapters.in.web.dto.CreateVisitRequest;
import com.jclinical.treatments.infra.adapters.in.web.dto.VisitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Registra sesiones clínicas (consumo real de material) que no están ligadas a ninguna
 * cotización, por ejemplo citas generales de la Agenda (revisión, urgencia, extracción de
 * emergencia) que consumen inventario sin formar parte de un plan de tratamiento presupuestado.
 */
@RestController
@RequestMapping("/api/v1/patients/{patientId}/visits")
@RequiredArgsConstructor
public class GeneralVisitController {

    private final ManageVisitsUseCase visitsUseCase;

    @PostMapping
    public ResponseEntity<VisitResponse> registerGeneralVisit(
            @PathVariable UUID patientId,
            @RequestBody CreateVisitRequest request) {
        RegisterVisitCommand command = new RegisterVisitCommand(
                request.visitDate(),
                request.doctorId(),
                request.notes(),
                toLineItemCommands(request.items())
        );
        Visit visit = visitsUseCase.registerVisit(patientId, null, request.clinicId(), command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(visit));
    }

    private List<RegisterVisitLineItemCommand> toLineItemCommands(List<CreateVisitRequest.CreateVisitLineItemRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream().map(req -> new RegisterVisitLineItemCommand(
                req.quotationItemId(),
                toMaterialCommands(req.materialsUsed())
        )).toList();
    }

    private List<RegisterVisitMaterialUsageCommand> toMaterialCommands(List<CreateVisitRequest.CreateVisitMaterialUsageRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream().map(req -> new RegisterVisitMaterialUsageCommand(
                req.materialId(),
                req.materialName(),
                req.actualQuantity()
        )).toList();
    }

    private VisitResponse toResponse(Visit visit) {
        return new VisitResponse(
                visit.getId(),
                visit.getClinicId(),
                visit.getPatientId(),
                visit.getQuotationId(),
                visit.getVisitDate(),
                visit.getDoctorId(),
                visit.getNotes(),
                visit.getItems().stream().map(this::toResponse).toList(),
                visit.getCreatedAt(),
                visit.getUpdatedAt()
        );
    }

    private VisitResponse.VisitLineItemResponse toResponse(VisitLineItem item) {
        return new VisitResponse.VisitLineItemResponse(
                item.getId(),
                item.getQuotationItemId(),
                item.getMaterialsUsed().stream().map(this::toResponse).toList()
        );
    }

    private VisitResponse.VisitMaterialUsageResponse toResponse(VisitMaterialUsage usage) {
        return new VisitResponse.VisitMaterialUsageResponse(
                usage.getId(),
                usage.getMaterialId(),
                usage.getMaterialName(),
                usage.getActualQuantity()
        );
    }
}
