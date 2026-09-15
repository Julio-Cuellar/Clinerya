package com.jclinical.treatments.domain.ports.in;

import com.jclinical.treatments.domain.model.ItemProgressStatus;
import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.model.QuotationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageQuotationUseCase {

    Quotation createQuotation(UUID patientId, UUID clinicId, UUID actingUserId, CreateQuotationCommand command);

    Quotation updateQuotationHeader(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId, UpdateHeaderCommand command);

    Quotation replaceQuotationItems(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId, ReplaceItemsCommand command);

    Quotation transitionStatus(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId, QuotationStatus targetStatus);

    Quotation updateItemProgress(UUID quotationId, UUID itemId, UUID patientId, UUID clinicId, UUID actingUserId, ItemProgressStatus targetStatus);

    Optional<Quotation> getQuotation(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId);

    List<Quotation> getQuotationsByPatient(UUID patientId, UUID clinicId, UUID actingUserId);

    void deleteQuotation(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId);

    record MaterialLineCommand(
        UUID materialId,
        String materialName,
        BigDecimal estimatedQuantity,
        BigDecimal manualUnitCost
    ) {}

    record QuotationItemCommand(
        UUID catalogItemId,
        String description,
        Integer toothNumber,
        BigDecimal laborCharge,
        List<MaterialLineCommand> materials,
        BigDecimal discountPercentage
    ) {}

    record CreateQuotationCommand(
        LocalDate quotationDate,
        String notes,
        LocalDate validUntil,
        List<QuotationItemCommand> items
    ) {}

    record UpdateHeaderCommand(
        String notes,
        LocalDate validUntil,
        LocalDate quotationDate
    ) {}

    record ReplaceItemsCommand(
        List<QuotationItemCommand> items
    ) {}
}
