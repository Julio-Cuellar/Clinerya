package com.jclinical.accounting.infra.adapters.out.persistence;

import com.jclinical.accounting.domain.model.OpeningBalanceSetup;

public interface OpeningBalanceSetupMapper {
    OpeningBalanceSetupEntity toEntity(OpeningBalanceSetup domain);

    OpeningBalanceSetup toDomain(OpeningBalanceSetupEntity entity);
}
