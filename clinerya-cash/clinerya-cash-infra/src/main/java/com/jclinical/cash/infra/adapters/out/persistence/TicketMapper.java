package com.jclinical.cash.infra.adapters.out.persistence;

import com.jclinical.cash.domain.model.Ticket;

public interface TicketMapper {

    TicketEntity toEntity(Ticket domain);

    Ticket toDomain(TicketEntity entity);
}
