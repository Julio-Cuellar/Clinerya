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

    Quotation createQuotation(UUID patientId, UUID clinicId, CreateQuotationCommand command);

    Quotation updateQuotationHeader(UUID quotationId, UUID patientId, UUID clinicId, UpdateHeaderCommand command);

    Quotation replaceQuotationItems(UUID quotationId, UUID patientId, UUID clinicId, ReplaceItemsCommand command);

    Quotation transitionStatus(UUID quotationId, UUID patientId, UUID clinicId, QuotationStatus targetStatus);

    Quotation updateItemProgress(UUID quotationId, UUID itemId, UUID patientId, UUID clinicId, ItemProgressStatus targetStatus);

    Optional<Quotation> getQuotation(UUID quotationId, UUID patientId, UUID clinicId);

    List<Quotation> getQuotationsByPatient(UUID patientId, UUID clinicId);

    void deleteQuotation(UUID quotationId, UUID patientId, UUID clinicId);

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
        UUID createdByUserId,
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
