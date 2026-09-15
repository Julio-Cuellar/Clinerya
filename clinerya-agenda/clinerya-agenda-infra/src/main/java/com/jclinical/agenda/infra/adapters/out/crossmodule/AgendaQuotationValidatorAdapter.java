package com.jclinical.agenda.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort;
import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.model.QuotationStatus;
import com.jclinical.treatments.domain.ports.in.QuotationLookupUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgendaQuotationValidatorAdapter implements QuotationValidatorPort {

    private final QuotationLookupUseCase quotationUseCase;

    @Override
    public Optional<AcceptedQuotationSnapshot> findAcceptedQuotation(UUID quotationId, UUID patientId, UUID clinicId) {
        return quotationUseCase.findQuotationForSystem(quotationId, patientId, clinicId)
                .filter(quotation -> quotation.getStatus() == QuotationStatus.ACCEPTED)
                .map(this::toSnapshot);
    }

    private AcceptedQuotationSnapshot toSnapshot(Quotation quotation) {
        var items = quotation.getItems() == null
                ? java.util.List.<QuotationItemSnapshot>of()
                : quotation.getItems().stream()
                        .map(item -> new QuotationItemSnapshot(item.getId(), item.getDescription(), toMaterialSnapshots(item)))
                        .toList();
        return new AcceptedQuotationSnapshot(quotation.getId(), items);
    }

    private java.util.List<QuotationValidatorPort.QuotationItemMaterialSnapshot> toMaterialSnapshots(
            com.jclinical.treatments.domain.model.QuotationItem item) {
        if (item.getMaterials() == null) {
            return java.util.List.of();
        }
        return item.getMaterials().stream()
                .map(material -> new QuotationValidatorPort.QuotationItemMaterialSnapshot(
                        material.getMaterialId(), material.getMaterialName(), material.getEstimatedQuantity()))
                .toList();
    }
}
