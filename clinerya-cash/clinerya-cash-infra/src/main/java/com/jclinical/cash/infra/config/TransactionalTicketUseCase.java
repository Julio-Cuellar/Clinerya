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
    public Ticket registerTicket(UUID clinicId, UUID actingUserId, RegisterTicketCommand command) {
        return ticketService.registerTicket(clinicId, actingUserId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicket(UUID ticketId, UUID clinicId, UUID actingUserId) {
        return ticketService.getTicket(ticketId, clinicId, actingUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listBySession(UUID cashSessionId, UUID clinicId, UUID actingUserId) {
        return ticketService.listBySession(cashSessionId, clinicId, actingUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listByClinicRange(UUID clinicId, UUID actingUserId, LocalDateTime from, LocalDateTime to) {
        return ticketService.listByClinicRange(clinicId, actingUserId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listByQuotation(UUID quotationId, UUID clinicId, UUID actingUserId) {
        return ticketService.listByQuotation(quotationId, clinicId, actingUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listByPatient(UUID patientId, UUID clinicId, UUID actingUserId) {
        return ticketService.listByPatient(patientId, clinicId, actingUserId);
    }

    @Override
    @Transactional
    public Ticket voidTicket(UUID ticketId, UUID clinicId, UUID actingUserId, VoidTicketCommand command) {
        return ticketService.voidTicket(ticketId, clinicId, actingUserId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationBalance getQuotationBalance(UUID quotationId, UUID patientId, UUID clinicId, UUID actingUserId) {
        return ticketService.getQuotationBalance(quotationId, patientId, clinicId, actingUserId);
    }
}
