package com.jclinical.agenda.domain.ports.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuotationValidatorPort {

    Optional<AcceptedQuotationSnapshot> findAcceptedQuotation(UUID quotationId, UUID patientId, UUID clinicId);

    record AcceptedQuotationSnapshot(
            UUID quotationId,
            List<QuotationItemSnapshot> items
    ) {}

    record QuotationItemSnapshot(
            UUID itemId,
            String description,
            List<QuotationItemMaterialSnapshot> materials
    ) {}

    record QuotationItemMaterialSnapshot(
            UUID materialId,
            String materialName,
            BigDecimal estimatedQuantity
    ) {}
}
