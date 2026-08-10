package com.jclinical.cash.domain.ports.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashQuotationValidatorPort {

    Optional<QuotationSnapshot> findQuotation(UUID quotationId, UUID patientId, UUID clinicId);

    record QuotationSnapshot(
            UUID quotationId,
            BigDecimal grandTotal,
            boolean accepted,
            List<QuotationItemSnapshot> items
    ) {}

    record QuotationItemSnapshot(UUID quotationItemId, String description, BigDecimal subtotal) {}
}
