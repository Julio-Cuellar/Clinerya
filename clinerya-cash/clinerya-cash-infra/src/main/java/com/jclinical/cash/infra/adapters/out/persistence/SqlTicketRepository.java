package com.jclinical.cash.infra.adapters.out.persistence;

import com.jclinical.cash.domain.model.PaymentMethod;
import com.jclinical.cash.domain.model.Ticket;
import com.jclinical.cash.domain.model.TicketStatus;
import com.jclinical.cash.domain.ports.out.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlTicketRepository implements TicketRepositoryPort {

    private final SpringDataTicketRepository springRepository;
    private final SpringDataCashFolioCounterRepository folioCounterRepository;
    private final TicketMapper mapper;

    @Override
    public Ticket save(Ticket ticket) {
        TicketEntity entity = mapper.toEntity(ticket);
        TicketEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Ticket> findByIdAndClinicId(UUID ticketId, UUID clinicId) {
        return springRepository.findByIdAndClinicId(ticketId, clinicId).map(mapper::toDomain);
    }

    @Override
    public List<Ticket> findByCashSessionId(UUID cashSessionId, UUID clinicId) {
        return springRepository.findByCashSessionIdAndClinicId(cashSessionId, clinicId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByClinicIdAndRange(UUID clinicId, LocalDateTime from, LocalDateTime to) {
        return springRepository.findByClinicIdAndRange(clinicId, from, to).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByQuotationIdAndClinicId(UUID quotationId, UUID clinicId) {
        return springRepository.findByQuotationIdAndClinicId(quotationId, clinicId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByPatientIdAndClinicId(UUID patientId, UUID clinicId) {
        return springRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public BigDecimal sumActiveCashAmountBySession(UUID cashSessionId) {
        BigDecimal sum = springRepository.sumActiveCashAmountBySession(cashSessionId, TicketStatus.ACTIVE, PaymentMethod.CASH);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumActiveAmountByQuotation(UUID quotationId, UUID clinicId) {
        BigDecimal sum = springRepository.sumActiveAmountByQuotation(quotationId, clinicId, TicketStatus.ACTIVE);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public Integer nextFolio(UUID clinicId) {
        CashFolioCounterEntity counter = folioCounterRepository.findById(clinicId)
                .orElseGet(() -> CashFolioCounterEntity.builder().clinicId(clinicId).lastFolio(0).build());
        int next = counter.getLastFolio() + 1;
        counter.setLastFolio(next);
        folioCounterRepository.save(counter);
        return next;
    }
}
