package com.jclinical.cash.infra.config;

import com.jclinical.cash.domain.model.CashExpense;
import com.jclinical.cash.domain.model.CashSession;
import com.jclinical.cash.domain.model.PaymentLine;
import com.jclinical.cash.domain.model.Ticket;
import com.jclinical.cash.domain.ports.out.CashBankAccountValidatorPort;
import com.jclinical.cash.domain.ports.out.CashAppointmentPort;
import com.jclinical.cash.domain.ports.out.CashExpenseRepositoryPort;
import com.jclinical.cash.domain.ports.out.CashPatientValidatorPort;
import com.jclinical.cash.domain.ports.out.CashQuotationValidatorPort;
import com.jclinical.cash.domain.ports.out.CashSessionRepositoryPort;
import com.jclinical.cash.domain.ports.out.CashStaffValidatorPort;
import com.jclinical.cash.domain.ports.out.TicketRepositoryPort;
import com.jclinical.cash.domain.service.CashExpenseService;
import com.jclinical.cash.domain.service.CashSessionService;
import com.jclinical.cash.domain.service.PendingAppointmentChargeService;
import com.jclinical.cash.domain.service.TicketService;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.cash.infra.adapters.out.persistence.CashExpenseEntity;
import com.jclinical.cash.infra.adapters.out.persistence.CashExpenseMapper;
import com.jclinical.cash.infra.adapters.out.persistence.CashSessionEntity;
import com.jclinical.cash.infra.adapters.out.persistence.CashSessionMapper;
import com.jclinical.cash.infra.adapters.out.persistence.PaymentLineEntity;
import com.jclinical.cash.infra.adapters.out.persistence.TicketEntity;
import com.jclinical.cash.infra.adapters.out.persistence.TicketMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CashDomainConfig {

    @Bean
    public CashSessionService cashSessionService(
            CashSessionRepositoryPort cashSessionRepository,
            TicketRepositoryPort ticketRepository,
            CashStaffValidatorPort staffValidator,
            CashExpenseRepositoryPort expenseRepository,
            StaffPermissionCheckerPort permissionChecker) {
        return new CashSessionService(
                cashSessionRepository, ticketRepository, staffValidator, expenseRepository, permissionChecker);
    }

    @Bean
    public TicketService ticketService(
            TicketRepositoryPort ticketRepository,
            CashSessionRepositoryPort cashSessionRepository,
            CashPatientValidatorPort patientValidator,
            CashQuotationValidatorPort quotationValidator,
            CashStaffValidatorPort staffValidator,
            CashBankAccountValidatorPort bankAccountValidator,
            com.jclinical.core.events.DomainEventPublisherPort eventPublisher,
            StaffPermissionCheckerPort permissionChecker) {
        return new TicketService(
                ticketRepository,
                cashSessionRepository,
                patientValidator,
                quotationValidator,
                staffValidator,
                bankAccountValidator,
                eventPublisher,
                permissionChecker);
    }

    @Bean
    public PendingAppointmentChargeService pendingAppointmentChargeService(
            CashAppointmentPort appointmentPort,
            CashQuotationValidatorPort quotationValidator,
            TicketRepositoryPort ticketRepository,
            StaffPermissionCheckerPort permissionChecker) {
        return new PendingAppointmentChargeService(
                appointmentPort, quotationValidator, ticketRepository, permissionChecker);
    }

    @Bean
    public CashExpenseService cashExpenseService(
            CashExpenseRepositoryPort expenseRepository,
            CashSessionRepositoryPort cashSessionRepository,
            CashStaffValidatorPort staffValidator,
            com.jclinical.core.events.DomainEventPublisherPort eventPublisher,
            StaffPermissionCheckerPort permissionChecker) {
        return new CashExpenseService(
                expenseRepository, cashSessionRepository, staffValidator, eventPublisher, permissionChecker);
    }

    @Bean
    @ConditionalOnMissingBean(CashSessionMapper.class)
    public CashSessionMapper cashSessionMapper() {
        return new CashSessionMapper() {
            @Override
            public CashSessionEntity toEntity(CashSession domain) {
                if (domain == null) {
                    return null;
                }
                return CashSessionEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .openedByStaffId(domain.getOpenedByStaffId())
                        .openedAt(domain.getOpenedAt())
                        .openingAmount(domain.getOpeningAmount())
                        .closedByStaffId(domain.getClosedByStaffId())
                        .closedAt(domain.getClosedAt())
                        .countedCashAmount(domain.getCountedCashAmount())
                        .expectedCashAmount(domain.getExpectedCashAmount())
                        .cashDifference(domain.getCashDifference())
                        .status(domain.getStatus())
                        .build();
            }

            @Override
            public CashSession toDomain(CashSessionEntity entity) {
                if (entity == null) {
                    return null;
                }
                return CashSession.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .openedByStaffId(entity.getOpenedByStaffId())
                        .openedAt(entity.getOpenedAt())
                        .openingAmount(entity.getOpeningAmount())
                        .closedByStaffId(entity.getClosedByStaffId())
                        .closedAt(entity.getClosedAt())
                        .countedCashAmount(entity.getCountedCashAmount())
                        .expectedCashAmount(entity.getExpectedCashAmount())
                        .cashDifference(entity.getCashDifference())
                        .status(entity.getStatus())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(TicketMapper.class)
    public TicketMapper ticketMapper() {
        return new TicketMapper() {
            @Override
            public TicketEntity toEntity(Ticket domain) {
                if (domain == null) {
                    return null;
                }
                TicketEntity ticketEntity = TicketEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .cashSessionId(domain.getCashSessionId())
                        .patientId(domain.getPatientId())
                        .quotationId(domain.getQuotationId())
                        .folio(domain.getFolio())
                        .totalAmount(domain.getTotalAmount())
                        .concept(domain.getConcept())
                        .createdByStaffId(domain.getCreatedByStaffId())
                        .createdAt(domain.getCreatedAt())
                        .status(domain.getStatus())
                        .voidedByStaffId(domain.getVoidedByStaffId())
                        .voidedAt(domain.getVoidedAt())
                        .voidReason(domain.getVoidReason())
                        .discountAmount(domain.getDiscountAmount())
                        .discountAuthorizedByStaffId(domain.getDiscountAuthorizedByStaffId())
                        .discountReason(domain.getDiscountReason())
                        .paymentLines(new ArrayList<>())
                        .build();

                List<PaymentLineEntity> paymentLines = domain.getPaymentLines() == null
                        ? new ArrayList<>()
                        : domain.getPaymentLines().stream()
                                .map(line -> toEntity(line, ticketEntity))
                                .toList();
                ticketEntity.setPaymentLines(new ArrayList<>(paymentLines));
                return ticketEntity;
            }

            private PaymentLineEntity toEntity(PaymentLine domain, TicketEntity ticketEntity) {
                return PaymentLineEntity.builder()
                        .id(domain.getId())
                        .ticket(ticketEntity)
                        .method(domain.getMethod())
                        .amount(domain.getAmount())
                        .reference(domain.getReference())
                        .bankAccountId(domain.getBankAccountId())
                        .build();
            }

            @Override
            public Ticket toDomain(TicketEntity entity) {
                if (entity == null) {
                    return null;
                }
                List<PaymentLine> paymentLines = entity.getPaymentLines() == null
                        ? new ArrayList<>()
                        : entity.getPaymentLines().stream()
                                .map(this::toDomain)
                                .toList();
                return Ticket.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .cashSessionId(entity.getCashSessionId())
                        .patientId(entity.getPatientId())
                        .quotationId(entity.getQuotationId())
                        .folio(entity.getFolio())
                        .totalAmount(entity.getTotalAmount())
                        .concept(entity.getConcept())
                        .createdByStaffId(entity.getCreatedByStaffId())
                        .createdAt(entity.getCreatedAt())
                        .status(entity.getStatus())
                        .voidedByStaffId(entity.getVoidedByStaffId())
                        .voidedAt(entity.getVoidedAt())
                        .voidReason(entity.getVoidReason())
                        .discountAmount(entity.getDiscountAmount())
                        .discountAuthorizedByStaffId(entity.getDiscountAuthorizedByStaffId())
                        .discountReason(entity.getDiscountReason())
                        .paymentLines(new ArrayList<>(paymentLines))
                        .build();
            }

            private PaymentLine toDomain(PaymentLineEntity entity) {
                return PaymentLine.builder()
                        .id(entity.getId())
                        .method(entity.getMethod())
                        .amount(entity.getAmount())
                        .reference(entity.getReference())
                        .bankAccountId(entity.getBankAccountId())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(CashExpenseMapper.class)
    public CashExpenseMapper cashExpenseMapper() {
        return new CashExpenseMapper() {
            @Override
            public CashExpenseEntity toEntity(CashExpense domain) {
                if (domain == null) {
                    return null;
                }
                return CashExpenseEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .cashSessionId(domain.getCashSessionId())
                        .concept(domain.getConcept())
                        .amount(domain.getAmount())
                        .createdByStaffId(domain.getCreatedByStaffId())
                        .createdAt(domain.getCreatedAt())
                        .status(domain.getStatus())
                        .voidedByStaffId(domain.getVoidedByStaffId())
                        .voidedAt(domain.getVoidedAt())
                        .voidReason(domain.getVoidReason())
                        .build();
            }

            @Override
            public CashExpense toDomain(CashExpenseEntity entity) {
                if (entity == null) {
                    return null;
                }
                return CashExpense.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .cashSessionId(entity.getCashSessionId())
                        .concept(entity.getConcept())
                        .amount(entity.getAmount())
                        .createdByStaffId(entity.getCreatedByStaffId())
                        .createdAt(entity.getCreatedAt())
                        .status(entity.getStatus())
                        .voidedByStaffId(entity.getVoidedByStaffId())
                        .voidedAt(entity.getVoidedAt())
                        .voidReason(entity.getVoidReason())
                        .build();
            }
        };
    }
}
