package com.jclinical.cash.infra.config;

import com.jclinical.cash.domain.model.Ticket;
import com.jclinical.cash.domain.ports.in.ManageTicketsUseCase;
import com.jclinical.cash.domain.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalTicketUseCase implements ManageTicketsUseCase {

    private final TicketService ticketService;

    @Override
    @Transactional
    public Ticket registerTicket(UUID actingUserId, UUID clinicId, RegisterTicketCommand command) {
        return ticketService.registerTicket(actingUserId, clinicId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicket(UUID actingUserId, UUID ticketId, UUID clinicId) {
        return ticketService.getTicket(actingUserId, ticketId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listBySession(UUID actingUserId, UUID cashSessionId, UUID clinicId) {
        return ticketService.listBySession(actingUserId, cashSessionId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listByClinicRange(UUID actingUserId, UUID clinicId, LocalDateTime from, LocalDateTime to) {
        return ticketService.listByClinicRange(actingUserId, clinicId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listByQuotation(UUID actingUserId, UUID quotationId, UUID clinicId) {
        return ticketService.listByQuotation(actingUserId, quotationId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listByPatient(UUID actingUserId, UUID patientId, UUID clinicId) {
        return ticketService.listByPatient(actingUserId, patientId, clinicId);
    }

    @Override
    @Transactional
    public Ticket voidTicket(UUID actingUserId, UUID ticketId, UUID clinicId, VoidTicketCommand command) {
        return ticketService.voidTicket(actingUserId, ticketId, clinicId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationBalance getQuotationBalance(UUID actingUserId, UUID quotationId, UUID patientId, UUID clinicId) {
        return ticketService.getQuotationBalance(actingUserId, quotationId, patientId, clinicId);
    }
}
