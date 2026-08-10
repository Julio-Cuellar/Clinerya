package com.jclinical.cash.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CloseCashSessionRequest(UUID closedByStaffId, BigDecimal countedCashAmount) {}
