package com.jclinical.cash.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLine {
    private UUID id;
    private PaymentMethod method;
    private BigDecimal amount;
    private String reference;
    private UUID bankAccountId;
}
