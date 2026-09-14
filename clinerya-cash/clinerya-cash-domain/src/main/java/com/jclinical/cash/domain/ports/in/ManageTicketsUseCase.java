package com.jclinical.cash.domain.ports.in;

import com.jclinical.cash.domain.model.PaymentMethod;
import com.jclinical.cash.domain.model.Ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageTicketsUseCase {

    Ticket registerTicket(UUID actingUserId, UUID clinicId, RegisterTicketCommand command);

    Ticket getTicket(UUID actingUserId, UUID ticketId, UUID clinicId);

    List<Ticket> listBySession(UUID actingUserId, UUID cashSessionId, UUID clinicId);

    List<Ticket> listByClinicRange(UUID actingUserId, UUID clinicId, LocalDateTime from, LocalDateTime to);

    List<Ticket> listByQuotation(UUID actingUserId, UUID quotationId, UUID clinicId);

    List<Ticket> listByPatient(UUID actingUserId, UUID patientId, UUID clinicId);

    Ticket voidTicket(UUID actingUserId, UUID ticketId, UUID clinicId, VoidTicketCommand command);

    QuotationBalance getQuotationBalance(UUID actingUserId, UUID quotationId, UUID patientId, UUID clinicId);

    record PaymentLineCommand(PaymentMethod method, BigDecimal amount, String reference, UUID bankAccountId) {}

    record RegisterTicketCommand(
            UUID patientId,
            UUID quotationId,
            String concept,
            UUID createdByStaffId,
            List<PaymentLineCommand> paymentLines,
            BigDecimal discountAmount,
            UUID discountAuthorizedByStaffId,
            String discountReason
    ) {}

    record VoidTicketCommand(UUID staffId, String reason) {}

    record QuotationBalance(UUID quotationId, BigDecimal grandTotal, BigDecimal paidAmount, BigDecimal remainingBalance) {}
}
