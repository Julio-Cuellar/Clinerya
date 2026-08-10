package com.jclinical.accounting.infra.adapters.out.persistence;

import com.jclinical.accounting.domain.model.BankAccount;

public interface BankAccountMapper {
    BankAccountEntity toEntity(BankAccount domain);

    BankAccount toDomain(BankAccountEntity entity);
}
