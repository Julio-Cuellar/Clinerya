package com.jclinical.cash.infra.adapters.in.web;

import com.jclinical.cash.domain.model.PaymentLine;
import com.jclinical.cash.domain.model.Ticket;
import com.jclinical.cash.domain.ports.in.ManageTicketsUseCase;
import com.jclinical.cash.domain.ports.in.ManagePendingAppointmentChargesUseCase;
import com.jclinical.cash.domain.ports.in.ManageTicketsUseCase.PaymentLineCommand;
import com.jclinical.cash.domain.ports.in.ManageTicketsUseCase.QuotationBalance;
import com.jclinical.cash.domain.ports.in.ManageTicketsUseCase.RegisterTicketCommand;
import com.jclinical.cash.domain.ports.in.ManageTicketsUseCase.VoidTicketCommand;
import com.jclinical.cash.infra.adapters.in.web.dto.PaymentLineResponse;
import com.jclinical.cash.infra.adapters.in.web.dto.PendingAppointmentChargeResponse;
import com.jclinical.cash.infra.adapters.in.web.dto.QuotationBalanceResponse;
import com.jclinical.cash.infra.adapters.in.web.dto.RegisterTicketRequest;
import com.jclinical.cash.infra.adapters.in.web.dto.TicketResponse;
import com.jclinical.cash.infra.adapters.in.web.dto.VoidTicketRequest;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final ManageTicketsUseCase ticketsUseCase;
    private final ManagePendingAppointmentChargesUseCase pendingAppointmentChargesUseCase;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("")
    public ResponseEntity<TicketResponse> registerTicket(
            @PathVariable UUID clinicId,
            @RequestBody RegisterTicketRequest request) {
        List<PaymentLineCommand> paymentLines = request.paymentLines() == null
                ? List.of()
                : request.paymentLines().stream()
                        .map(line -> new PaymentLineCommand(line.method(), line.amount(), line.reference(), line.bankAccountId()))
                        .toList();
        RegisterTicketCommand command = new RegisterTicketCommand(
                request.patientId(),
                request.quotationId(),
                request.concept(),
                request.createdByStaffId(),
                paymentLines,
                request.discountAmount(),
                request.discountAuthorizedByStaffId(),
                request.discountReason()
        );
        Ticket ticket = ticketsUseCase.registerTicket(currentUserResolver.getCurrentUserId(), clinicId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(ticket));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicket(
            @PathVariable UUID clinicId,
            @PathVariable UUID ticketId) {
        Ticket ticket = ticketsUseCase.getTicket(currentUserResolver.getCurrentUserId(), ticketId, clinicId);
        return ResponseEntity.ok(toResponse(ticket));
    }

    @GetMapping("")
    public ResponseEntity<List<TicketResponse>> listByClinicRange(
            @PathVariable UUID clinicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<TicketResponse> responses = ticketsUseCase
                .listByClinicRange(currentUserResolver.getCurrentUserId(), clinicId, from, to).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-session/{cashSessionId}")
    public ResponseEntity<List<TicketResponse>> listBySession(
            @PathVariable UUID clinicId,
            @PathVariable UUID cashSessionId) {
        List<TicketResponse> responses = ticketsUseCase
                .listBySession(currentUserResolver.getCurrentUserId(), cashSessionId, clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-quotation/{quotationId}")
    public ResponseEntity<List<TicketResponse>> listByQuotation(
            @PathVariable UUID clinicId,
            @PathVariable UUID quotationId) {
        List<TicketResponse> responses = ticketsUseCase
                .listByQuotation(currentUserResolver.getCurrentUserId(), quotationId, clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-patient/{patientId}")
    public ResponseEntity<List<TicketResponse>> listByPatient(
            @PathVariable UUID clinicId,
            @PathVariable UUID patientId) {
        List<TicketResponse> responses = ticketsUseCase
                .listByPatient(currentUserResolver.getCurrentUserId(), patientId, clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/pending-appointment-charges")
    public ResponseEntity<List<PendingAppointmentChargeResponse>> listPendingAppointmentCharges(
            @PathVariable UUID clinicId) {
        List<PendingAppointmentChargeResponse> responses = pendingAppointmentChargesUseCase
                .listPendingCharges(currentUserResolver.getCurrentUserId(), clinicId)
                .stream()
                .map(charge -> new PendingAppointmentChargeResponse(
                        charge.appointmentId(),
                        charge.patientId(),
                        charge.doctorStaffId(),
                        charge.quotationId(),
                        charge.quotationItemId(),
                        charge.concept(),
                        charge.amount(),
                        charge.completedAt()))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{ticketId}/void")
    public ResponseEntity<TicketResponse> voidTicket(
            @PathVariable UUID clinicId,
            @PathVariable UUID ticketId,
            @RequestBody VoidTicketRequest request) {
        VoidTicketCommand command = new VoidTicketCommand(request.staffId(), request.reason());
        Ticket ticket = ticketsUseCase.voidTicket(currentUserResolver.getCurrentUserId(), ticketId, clinicId, command);
        return ResponseEntity.ok(toResponse(ticket));
    }

    @GetMapping("/quotation-balance/{quotationId}")
    public ResponseEntity<QuotationBalanceResponse> getQuotationBalance(
            @PathVariable UUID clinicId,
            @PathVariable UUID quotationId,
            @RequestParam UUID patientId) {
        QuotationBalance balance = ticketsUseCase.getQuotationBalance(
                currentUserResolver.getCurrentUserId(), quotationId, patientId, clinicId);
        return ResponseEntity.ok(new QuotationBalanceResponse(
                balance.quotationId(), balance.grandTotal(), balance.paidAmount(), balance.remainingBalance()));
    }

    private TicketResponse toResponse(Ticket ticket) {
        List<PaymentLineResponse> paymentLines = ticket.getPaymentLines() == null
                ? List.of()
                : ticket.getPaymentLines().stream()
                        .map(this::toResponse)
                        .toList();
        return new TicketResponse(
                ticket.getId(),
                ticket.getClinicId(),
                ticket.getCashSessionId(),
                ticket.getPatientId(),
                ticket.getQuotationId(),
                ticket.getFolio(),
                ticket.getTotalAmount(),
                ticket.getConcept(),
                ticket.getCreatedByStaffId(),
                ticket.getCreatedAt(),
                ticket.getStatus(),
                ticket.getVoidedByStaffId(),
                ticket.getVoidedAt(),
                ticket.getVoidReason(),
                ticket.getDiscountAmount(),
                ticket.getDiscountAuthorizedByStaffId(),
                ticket.getDiscountReason(),
                paymentLines
        );
    }

    private PaymentLineResponse toResponse(PaymentLine paymentLine) {
        return new PaymentLineResponse(
                paymentLine.getId(),
                paymentLine.getMethod(),
                paymentLine.getAmount(),
                paymentLine.getReference(),
                paymentLine.getBankAccountId());
    }
}
