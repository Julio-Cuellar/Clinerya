package com.jclinical.staff.domain.ports.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface PayrollAccountingPort {
    PaymentResult registerPayrollPayment(UUID clinicId, UUID payrollPeriodId, UUID bankAccountId,
                                         BigDecimal grossAmount, BigDecimal netAmount,
                                         BigDecimal deductionAmount, LocalDate paymentDate);

    record PaymentResult(UUID journalEntryId) {}
}
