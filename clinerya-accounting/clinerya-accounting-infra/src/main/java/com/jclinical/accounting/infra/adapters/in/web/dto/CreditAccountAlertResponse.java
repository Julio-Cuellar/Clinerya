package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreditAccountAlertResponse(
        UUID accountId,
        String accountName,
        String type,
        LocalDate alertDate,
        long daysRemaining,
        String severity,
        String title,
        String detail
) {}
