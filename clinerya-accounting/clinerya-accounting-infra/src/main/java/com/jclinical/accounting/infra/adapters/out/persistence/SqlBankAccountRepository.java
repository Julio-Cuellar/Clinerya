package com.jclinical.accounting.infra.adapters.out.persistence;

import com.jclinical.accounting.domain.model.BankAccount;
import com.jclinical.accounting.domain.ports.out.BankAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlBankAccountRepository implements BankAccountRepositoryPort {

    private final SpringDataBankAccountRepository springRepository;
    private final BankAccountMapper mapper;

    @Override
    public List<BankAccount> saveAll(List<BankAccount> bankAccounts) {
        List<BankAccountEntity> entities = bankAccounts.stream().map(mapper::toEntity).toList();
        return springRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public BankAccount save(BankAccount bankAccount) {
        return mapper.toDomain(springRepository.save(mapper.toEntity(bankAccount)));
    }

    @Override
    public List<BankAccount> findByClinicId(UUID clinicId) {
        return springRepository.findByClinicIdOrderByCreatedAtAsc(clinicId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<BankAccount> findByIdAndClinicId(UUID bankAccountId, UUID clinicId) {
        return springRepository.findByIdAndClinicId(bankAccountId, clinicId).map(mapper::toDomain);
    }
}
