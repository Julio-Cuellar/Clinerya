package com.jclinical.cash.infra.config;

import com.jclinical.cash.domain.model.CashSession;
import com.jclinical.cash.domain.ports.in.ManageCashSessionUseCase;
import com.jclinical.cash.domain.service.CashSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalCashSessionUseCase implements ManageCashSessionUseCase {

    private final CashSessionService cashSessionService;

    @Override
    @Transactional
    public CashSession openSession(UUID clinicId, UUID actingUserId, OpenSessionCommand command) {
        return cashSessionService.openSession(clinicId, actingUserId, command);
    }

    @Override
    @Transactional
    public CashSession closeSession(UUID clinicId, UUID actingUserId, CloseSessionCommand command) {
        return cashSessionService.closeSession(clinicId, actingUserId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CashSession> getCurrentSession(UUID clinicId, UUID actingUserId) {
        return cashSessionService.getCurrentSession(clinicId, actingUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public CashSession getSession(UUID sessionId, UUID clinicId, UUID actingUserId) {
        return cashSessionService.getSession(sessionId, clinicId, actingUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashSession> listSessions(UUID clinicId, UUID actingUserId) {
        return cashSessionService.listSessions(clinicId, actingUserId);
    }
}
