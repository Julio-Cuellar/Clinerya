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
    public CashSession openSession(UUID actingUserId, UUID clinicId, OpenSessionCommand command) {
        return cashSessionService.openSession(actingUserId, clinicId, command);
    }

    @Override
    @Transactional
    public CashSession closeSession(UUID actingUserId, UUID clinicId, CloseSessionCommand command) {
        return cashSessionService.closeSession(actingUserId, clinicId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CashSession> getCurrentSession(UUID actingUserId, UUID clinicId) {
        return cashSessionService.getCurrentSession(actingUserId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public CashSession getSession(UUID actingUserId, UUID sessionId, UUID clinicId) {
        return cashSessionService.getSession(actingUserId, sessionId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashSession> listSessions(UUID actingUserId, UUID clinicId) {
        return cashSessionService.listSessions(actingUserId, clinicId);
    }
}
