package com.jclinical.cash.domain.ports.out;

import com.jclinical.cash.domain.model.Ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepositoryPort {

    Ticket save(Ticket ticket);

    Optional<Ticket> findByIdAndClinicId(UUID ticketId, UUID clinicId);

    List<Ticket> findByCashSessionId(UUID cashSessionId, UUID clinicId);

    List<Ticket> findByClinicIdAndRange(UUID clinicId, LocalDateTime from, LocalDateTime to);

    List<Ticket> findByQuotationIdAndClinicId(UUID quotationId, UUID clinicId);

    List<Ticket> findByPatientIdAndClinicId(UUID patientId, UUID clinicId);

    BigDecimal sumActiveCashAmountBySession(UUID cashSessionId);

    BigDecimal sumActiveAmountByQuotation(UUID quotationId, UUID clinicId);

    Integer nextFolio(UUID clinicId);
}
