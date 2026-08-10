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
    public Ticket registerTicket(UUID clinicId, RegisterTicketCommand command) {
        return ticketService.registerTicket(clinicId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicket(UUID ticketId, UUID clinicId) {
        return ticketService.getTicket(ticketId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listBySession(UUID cashSessionId, UUID clinicId) {
        return ticketService.listBySession(cashSessionId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listByClinicRange(UUID clinicId, LocalDateTime from, LocalDateTime to) {
        return ticketService.listByClinicRange(clinicId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listByQuotation(UUID quotationId, UUID clinicId) {
        return ticketService.listByQuotation(quotationId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listByPatient(UUID patientId, UUID clinicId) {
        return ticketService.listByPatient(patientId, clinicId);
    }

    @Override
    @Transactional
    public Ticket voidTicket(UUID ticketId, UUID clinicId, VoidTicketCommand command) {
        return ticketService.voidTicket(ticketId, clinicId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationBalance getQuotationBalance(UUID quotationId, UUID patientId, UUID clinicId) {
        return ticketService.getQuotationBalance(quotationId, patientId, clinicId);
    }
}
