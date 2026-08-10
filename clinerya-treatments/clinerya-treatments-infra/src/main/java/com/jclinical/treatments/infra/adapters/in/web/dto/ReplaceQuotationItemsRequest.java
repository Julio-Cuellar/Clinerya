package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.util.List;
import java.util.UUID;

public record ReplaceQuotationItemsRequest(
    UUID clinicId,
    List<QuotationItemRequest> items
) {}
