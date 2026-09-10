package com.jclinical.staff.infra.adapters.out.crossmodule;

import com.jclinical.accounting.domain.model.BankAccount;
import com.jclinical.accounting.domain.model.BankAccountType;
import com.jclinical.accounting.domain.ports.in.ManageJournalUseCase;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase;
import com.jclinical.core.events.PayrollPaymentRegisteredEvent;
import com.jclinical.staff.domain.ports.out.PayrollAccountingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PayrollAccountingAdapter implements PayrollAccountingPort {

    private final ManageOpeningBalancesUseCase openingBalancesUseCase;
    private final ManageJournalUseCase journalUseCase;

    @Override
    public PaymentResult registerPayrollPayment(UUID clinicId, UUID payrollPeriodId, UUID bankAccountId,
                                                BigDecimal grossAmount, BigDecimal netAmount,
                                                BigDecimal deductionAmount, LocalDate paymentDate) {
        BankAccount account = openingBalancesUseCase.listBankAccountsForSystem(clinicId).stream()
                .filter(candidate -> bankAccountId.equals(candidate.getId()))
                .filter(BankAccount::isActive)
                .filter(candidate -> candidate.getAccountType() == BankAccountType.DEBIT)
                .findFirst()
                  .orElseThrow(() -> new IllegalArgumentException("La cuenta operativa seleccionada no existe, esta inactiva o no es de debito."));

        PayrollPaymentRegisteredEvent event = new PayrollPaymentRegisteredEvent(
                UUID.randomUUID(),
                clinicId,
                payrollPeriodId,
                account.getId(),
                account.getAccountCode(),
                account.displayName(),
                grossAmount,
                netAmount,
                deductionAmount,
                paymentDate);
        return journalUseCase.recordPayrollPayment(event)
                .map(entry -> new PaymentResult(entry.getId()))
                .orElseThrow(() -> new IllegalStateException("No se pudo generar la poliza del pago de nomina."));
    }
}
