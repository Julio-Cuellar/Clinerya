package com.jclinical.cash.infra.adapters.out.persistence;

import com.jclinical.cash.domain.model.CashSession;

public interface CashSessionMapper {

    CashSessionEntity toEntity(CashSession domain);

    CashSession toDomain(CashSessionEntity entity);
}
