package com.jclinical.accounting.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public record CreditAccountAlert(
        UUID accountId,
        String accountName,
        AlertType type,
        LocalDate alertDate,
        long daysRemaining,
        String severity,
        String title,
        String detail
) {

    public enum AlertType {
        CUTOFF,
        PAYMENT_DUE
    }
}
