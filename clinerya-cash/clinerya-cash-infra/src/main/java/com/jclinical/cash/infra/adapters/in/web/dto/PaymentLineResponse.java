package com.jclinical.cash.infra.adapters.in.web.dto;

import com.jclinical.cash.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentLineResponse(UUID id, PaymentMethod method, BigDecimal amount, String reference, UUID bankAccountId) {}
