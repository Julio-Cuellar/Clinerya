package com.jclinical.cash.infra.adapters.out.persistence;

import com.jclinical.cash.domain.model.CashSession;
import com.jclinical.cash.domain.model.CashSessionStatus;
import com.jclinical.cash.domain.ports.out.CashSessionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlCashSessionRepository implements CashSessionRepositoryPort {

    private final SpringDataCashSessionRepository springRepository;
    private final CashSessionMapper mapper;

    @Override
    public CashSession save(CashSession session) {
        CashSessionEntity entity = mapper.toEntity(session);
        CashSessionEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CashSession> findOpenByClinicId(UUID clinicId) {
        return springRepository.findByClinicIdAndStatus(clinicId, CashSessionStatus.OPEN).map(mapper::toDomain);
    }

    @Override
    public Optional<CashSession> findOpenByClinicIdForUpdate(UUID clinicId) {
        return springRepository.findByClinicIdAndStatusForUpdate(clinicId, CashSessionStatus.OPEN).map(mapper::toDomain);
    }

    @Override
    public Optional<CashSession> findByIdAndClinicId(UUID sessionId, UUID clinicId) {
        return springRepository.findByIdAndClinicId(sessionId, clinicId).map(mapper::toDomain);
    }

    @Override
    public List<CashSession> findByClinicId(UUID clinicId) {
        return springRepository.findByClinicIdOrderByOpenedAtDesc(clinicId).stream().map(mapper::toDomain).toList();
    }
}
