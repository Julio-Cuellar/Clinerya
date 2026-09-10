package com.jclinical.treatments.domain.ports.in;

import com.jclinical.treatments.domain.model.Visit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ManageVisitsUseCase {

    Visit registerVisit(UUID patientId, UUID quotationId, UUID clinicId, UUID actingUserId, RegisterVisitCommand command);

    List<Visit> getVisitsByQuotation(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId);

    Visit getVisitDetails(UUID visitId, UUID patientId, UUID clinicId, UUID actingUserId);

    record RegisterVisitCommand(
            LocalDate visitDate,
            UUID doctorId,
            String notes,
            List<RegisterVisitLineItemCommand> items
    ) {}

    record RegisterVisitLineItemCommand(
            UUID quotationItemId,
            List<RegisterVisitMaterialUsageCommand> materialsUsed
    ) {}

    record RegisterVisitMaterialUsageCommand(
            UUID materialId,
            String materialName,
            BigDecimal actualQuantity
    ) {}
}
