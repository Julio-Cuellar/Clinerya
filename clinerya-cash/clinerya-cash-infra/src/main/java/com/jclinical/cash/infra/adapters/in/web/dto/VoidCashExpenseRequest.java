package com.jclinical.cash.infra.adapters.in.web.dto;

import java.util.UUID;

public record VoidCashExpenseRequest(UUID staffId, String reason) {}
