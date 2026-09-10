package com.jclinical.cash.infra.adapters.out.crossmodule;

import com.jclinical.cash.domain.ports.out.CashQuotationValidatorPort;
import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.model.QuotationStatus;
import com.jclinical.treatments.domain.ports.in.QuotationLookupUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CashQuotationValidatorAdapter implements CashQuotationValidatorPort {

    private final QuotationLookupUseCase quotationUseCase;

    @Override
    public Optional<QuotationSnapshot> findQuotation(UUID quotationId, UUID patientId, UUID clinicId) {
        return quotationUseCase.findQuotationForSystem(quotationId, patientId, clinicId).map(this::toSnapshot);
    }

    private QuotationSnapshot toSnapshot(Quotation quotation) {
        return new QuotationSnapshot(
                quotation.getId(),
                quotation.grandTotal(),
                quotation.getStatus() == QuotationStatus.ACCEPTED,
                quotation.getItems().stream()
                        .map(item -> new QuotationItemSnapshot(item.getId(), item.getDescription(), item.subtotal()))
                        .toList()
        );
    }
}
