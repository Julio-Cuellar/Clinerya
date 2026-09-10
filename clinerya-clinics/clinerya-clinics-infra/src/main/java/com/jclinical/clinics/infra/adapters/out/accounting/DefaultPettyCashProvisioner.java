package com.jclinical.clinics.infra.adapters.out.accounting;

import com.jclinical.accounting.domain.model.BankAccountType;
import com.jclinical.accounting.domain.model.OperationalAccountKind;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultPettyCashProvisioner {

    private static final String DEFAULT_ALIAS = "Caja chica general";

    private final ManageOpeningBalancesUseCase openingBalancesUseCase;
    private final JdbcTemplate jdbcTemplate;

    public void ensureForClinic(UUID clinicId) {
        if (clinicId == null) {
            return;
        }
        boolean alreadyExists = openingBalancesUseCase.listBankAccountsForSystem(clinicId).stream()
                .anyMatch(account -> account.getAccountKind() == OperationalAccountKind.PETTY_CASH);
        if (alreadyExists) {
            return;
        }

        openingBalancesUseCase.createBankAccountForSystem(
                clinicId,
                new ManageOpeningBalancesUseCase.CreateBankAccountCommand(
                        DEFAULT_ALIAS,
                        DEFAULT_ALIAS,
                        null,
                        "MXN",
                        BigDecimal.ZERO,
                        BankAccountType.DEBIT,
                        OperationalAccountKind.PETTY_CASH,
                        LocalDate.now(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Cuenta predeterminada para controlar la caja chica"
                )
        );
        log.info("Caja chica predeterminada creada para la clinica {}", clinicId);
    }

    public void ensureForOwner(UUID ownerUserId) {
        if (ownerUserId == null) {
            return;
        }
        jdbcTemplate.query(
                "SELECT id FROM clinics.clinics WHERE owner_user_id = ?",
                (resultSet, rowNum) -> resultSet.getObject("id", UUID.class),
                ownerUserId
        ).forEach(this::ensureForClinic);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void provisionExistingClinics() {
        List<UUID> clinicIds = jdbcTemplate.query(
                "SELECT id FROM clinics.clinics",
                (resultSet, rowNum) -> resultSet.getObject("id", UUID.class)
        );
        clinicIds.forEach(this::ensureForClinic);
    }
}
