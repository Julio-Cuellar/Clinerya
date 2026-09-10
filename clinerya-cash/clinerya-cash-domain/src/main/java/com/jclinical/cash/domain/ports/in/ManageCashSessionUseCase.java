package com.jclinical.cash.domain.ports.in;

import com.jclinical.cash.domain.model.CashSession;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageCashSessionUseCase {

    CashSession openSession(UUID actingUserId, UUID clinicId, OpenSessionCommand command);

    CashSession closeSession(UUID actingUserId, UUID clinicId, CloseSessionCommand command);

    Optional<CashSession> getCurrentSession(UUID actingUserId, UUID clinicId);

    CashSession getSession(UUID actingUserId, UUID sessionId, UUID clinicId);

    List<CashSession> listSessions(UUID actingUserId, UUID clinicId);

    record OpenSessionCommand(UUID openedByStaffId, BigDecimal openingAmount) {}

    record CloseSessionCommand(UUID closedByStaffId, BigDecimal countedCashAmount) {}
}
