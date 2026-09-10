package com.jclinical.accounting.infra.config;

import com.jclinical.accounting.domain.model.BankAccount;
import com.jclinical.accounting.domain.model.BankAccountType;
import com.jclinical.accounting.domain.model.OperationalAccountKind;
import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalLine;
import com.jclinical.accounting.domain.model.OpeningBalanceSetup;
import com.jclinical.accounting.domain.ports.out.BankAccountRepositoryPort;
import com.jclinical.accounting.domain.ports.out.JournalEntryRepositoryPort;
import com.jclinical.accounting.domain.ports.out.OpeningBalanceSetupRepositoryPort;
import com.jclinical.accounting.domain.service.JournalEntryService;
import com.jclinical.accounting.domain.service.IncomeStatementService;
import com.jclinical.accounting.domain.service.OpeningBalanceService;
import com.jclinical.accounting.infra.adapters.out.persistence.BankAccountEntity;
import com.jclinical.accounting.infra.adapters.out.persistence.BankAccountMapper;
import com.jclinical.accounting.infra.adapters.out.persistence.JournalEntryEntity;
import com.jclinical.accounting.infra.adapters.out.persistence.JournalEntryMapper;
import com.jclinical.accounting.infra.adapters.out.persistence.JournalLineEntity;
import com.jclinical.accounting.infra.adapters.out.persistence.OpeningBalanceSetupEntity;
import com.jclinical.accounting.infra.adapters.out.persistence.OpeningBalanceSetupMapper;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AccountingDomainConfig {

    @Bean
    public JournalEntryService journalEntryService(JournalEntryRepositoryPort repository,
                                                  StaffPermissionCheckerPort permissionChecker) {
        return new JournalEntryService(repository, permissionChecker);
    }

    @Bean
    public IncomeStatementService incomeStatementService(JournalEntryRepositoryPort repository,
                                                        StaffPermissionCheckerPort permissionChecker) {
        return new IncomeStatementService(repository, permissionChecker);
    }

    @Bean
    public OpeningBalanceService openingBalanceService(
            OpeningBalanceSetupRepositoryPort setupRepository,
            BankAccountRepositoryPort bankAccountRepository,
            JournalEntryRepositoryPort journalEntryRepository,
            StaffPermissionCheckerPort permissionChecker) {
        return new OpeningBalanceService(setupRepository, bankAccountRepository, journalEntryRepository, permissionChecker);
    }

    @Bean
    @ConditionalOnMissingBean(JournalEntryMapper.class)
    public JournalEntryMapper journalEntryMapper() {
        return new JournalEntryMapper() {
            @Override
            public JournalEntryEntity toEntity(JournalEntry domain) {
                if (domain == null) {
                    return null;
                }
                JournalEntryEntity entryEntity = JournalEntryEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .description(domain.getDescription())
                        .entryDate(domain.getEntryDate())
                        .sourceEventType(domain.getSourceEventType())
                        .sourceEventId(domain.getSourceEventId())
                        .createdAt(domain.getCreatedAt())
                        .lines(new ArrayList<>())
                        .build();

                List<JournalLineEntity> lines = domain.getLines() == null
                        ? new ArrayList<>()
                        : domain.getLines().stream().map(line -> toEntity(line, entryEntity)).toList();
                entryEntity.setLines(new ArrayList<>(lines));
                return entryEntity;
            }

            private JournalLineEntity toEntity(JournalLine domain, JournalEntryEntity entryEntity) {
                return JournalLineEntity.builder()
                        .id(domain.getId())
                        .journalEntry(entryEntity)
                        .bankAccountId(domain.getBankAccountId())
                        .accountCode(domain.getAccountCode())
                        .accountName(domain.getAccountName())
                        .debit(domain.getDebit())
                        .credit(domain.getCredit())
                        .build();
            }

            @Override
            public JournalEntry toDomain(JournalEntryEntity entity) {
                if (entity == null) {
                    return null;
                }
                List<JournalLine> lines = entity.getLines() == null
                        ? new ArrayList<>()
                        : entity.getLines().stream().map(this::toDomain).toList();
                return JournalEntry.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .description(entity.getDescription())
                        .entryDate(entity.getEntryDate())
                        .sourceEventType(entity.getSourceEventType())
                        .sourceEventId(entity.getSourceEventId())
                        .createdAt(entity.getCreatedAt())
                        .lines(new ArrayList<>(lines))
                        .build();
            }

            private JournalLine toDomain(JournalLineEntity entity) {
                return JournalLine.builder()
                        .id(entity.getId())
                        .bankAccountId(entity.getBankAccountId())
                        .accountCode(entity.getAccountCode())
                        .accountName(entity.getAccountName())
                        .debit(entity.getDebit())
                        .credit(entity.getCredit())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(BankAccountMapper.class)
    public BankAccountMapper bankAccountMapper() {
        return new BankAccountMapper() {
            @Override
            public BankAccountEntity toEntity(BankAccount domain) {
                if (domain == null) {
                    return null;
                }
                return BankAccountEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .openingBalanceSetupId(domain.getOpeningBalanceSetupId())
                        .openingJournalEntryId(domain.getOpeningJournalEntryId())
                        .accountCode(domain.getAccountCode())
                        .accountType(domain.getAccountType() != null ? domain.getAccountType().name() : BankAccountType.DEBIT.name())
                        .accountKind(domain.getAccountKind() != null ? domain.getAccountKind().name() : OperationalAccountKind.BANK.name())
                        .bankName(domain.getBankName())
                        .alias(domain.getAlias())
                        .accountLast4(domain.getAccountLast4())
                        .currency(domain.getCurrency())
                        .openingBalance(domain.getOpeningBalance())
                        .openingDate(domain.getOpeningDate())
                        .creditCutoffDate(domain.getCreditCutoffDate())
                        .creditPaymentDueDate(domain.getCreditPaymentDueDate())
                        .creditLimit(domain.getCreditLimit())
                        .creditCurrentAmount(domain.getCreditCurrentAmount())
                        .creditMinimumPayment(domain.getCreditMinimumPayment())
                        .creditNoInterestPayment(domain.getCreditNoInterestPayment())
                        .creditCurrentPaymentDue(domain.getCreditCurrentPaymentDue())
                        .active(domain.isActive())
                        .deactivatedAt(domain.getDeactivatedAt())
                        .deactivationReason(domain.getDeactivationReason())
                        .lastModifiedAt(domain.getLastModifiedAt())
                        .lastModificationReason(domain.getLastModificationReason())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public BankAccount toDomain(BankAccountEntity entity) {
                if (entity == null) {
                    return null;
                }
                return BankAccount.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .openingBalanceSetupId(entity.getOpeningBalanceSetupId())
                        .openingJournalEntryId(entity.getOpeningJournalEntryId())
                        .accountCode(entity.getAccountCode())
                        .accountType(entity.getAccountType() != null ? BankAccountType.valueOf(entity.getAccountType()) : BankAccountType.DEBIT)
                        .accountKind(entity.getAccountKind() != null ? OperationalAccountKind.valueOf(entity.getAccountKind()) : OperationalAccountKind.BANK)
                        .bankName(entity.getBankName())
                        .alias(entity.getAlias())
                        .accountLast4(entity.getAccountLast4())
                        .currency(entity.getCurrency())
                        .openingBalance(entity.getOpeningBalance())
                        .openingDate(entity.getOpeningDate())
                        .creditCutoffDate(entity.getCreditCutoffDate())
                        .creditPaymentDueDate(entity.getCreditPaymentDueDate())
                        .creditLimit(entity.getCreditLimit())
                        .creditCurrentAmount(entity.getCreditCurrentAmount())
                        .creditMinimumPayment(entity.getCreditMinimumPayment())
                        .creditNoInterestPayment(entity.getCreditNoInterestPayment())
                        .creditCurrentPaymentDue(entity.getCreditCurrentPaymentDue())
                        .active(entity.isActive())
                        .deactivatedAt(entity.getDeactivatedAt())
                        .deactivationReason(entity.getDeactivationReason())
                        .lastModifiedAt(entity.getLastModifiedAt())
                        .lastModificationReason(entity.getLastModificationReason())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(OpeningBalanceSetupMapper.class)
    public OpeningBalanceSetupMapper openingBalanceSetupMapper() {
        return new OpeningBalanceSetupMapper() {
            @Override
            public OpeningBalanceSetupEntity toEntity(OpeningBalanceSetup domain) {
                if (domain == null) {
                    return null;
                }
                return OpeningBalanceSetupEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .entryDate(domain.getEntryDate())
                        .cashOpeningAmount(domain.getCashOpeningAmount())
                        .inventoryOpeningAmount(domain.getInventoryOpeningAmount())
                        .totalOpeningAssets(domain.getTotalOpeningAssets())
                        .capitalAccountCode(domain.getCapitalAccountCode())
                        .capitalAccountName(domain.getCapitalAccountName())
                        .journalEntryId(domain.getJournalEntryId())
                        .notes(domain.getNotes())
                        .createdAt(domain.getCreatedAt())
                        .build();
            }

            @Override
            public OpeningBalanceSetup toDomain(OpeningBalanceSetupEntity entity) {
                if (entity == null) {
                    return null;
                }
                return OpeningBalanceSetup.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .entryDate(entity.getEntryDate())
                        .cashOpeningAmount(entity.getCashOpeningAmount())
                        .inventoryOpeningAmount(entity.getInventoryOpeningAmount())
                        .totalOpeningAssets(entity.getTotalOpeningAssets())
                        .capitalAccountCode(entity.getCapitalAccountCode())
                        .capitalAccountName(entity.getCapitalAccountName())
                        .journalEntryId(entity.getJournalEntryId())
                        .notes(entity.getNotes())
                        .createdAt(entity.getCreatedAt())
                        .build();
            }
        };
    }
}
