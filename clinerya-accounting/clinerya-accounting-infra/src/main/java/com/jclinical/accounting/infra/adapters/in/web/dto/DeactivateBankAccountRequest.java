package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.time.LocalDate;

public record DeactivateBankAccountRequest(
        LocalDate entryDate,
        String reason
) {}
