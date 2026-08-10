package com.jclinical.cash.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QuotationBalanceResponse(UUID quotationId, BigDecimal grandTotal, BigDecimal paidAmount, BigDecimal remainingBalance) {}
