package com.jclinical.cash.domain.ports.in;

import com.jclinical.cash.domain.model.PaymentMethod;
import com.jclinical.cash.domain.model.Ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageTicketsUseCase {

    Ticket registerTicket(UUID clinicId, UUID actingUserId, RegisterTicketCommand command);

    Ticket getTicket(UUID ticketId, UUID clinicId, UUID actingUserId);

    List<Ticket> listBySession(UUID cashSessionId, UUID clinicId, UUID actingUserId);

    List<Ticket> listByClinicRange(UUID clinicId, UUID actingUserId, LocalDateTime from, LocalDateTime to);

    List<Ticket> listByQuotation(UUID quotationId, UUID clinicId, UUID actingUserId);

    List<Ticket> listByPatient(UUID patientId, UUID clinicId, UUID actingUserId);

    Ticket voidTicket(UUID ticketId, UUID clinicId, UUID actingUserId, VoidTicketCommand command);

    QuotationBalance getQuotationBalance(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId);

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
