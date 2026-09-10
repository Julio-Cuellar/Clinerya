package com.jclinical.cash.domain.ports.in;

import com.jclinical.cash.domain.model.CashSession;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageCashSessionUseCase {

    CashSession openSession(UUID clinicId, UUID actingUserId, OpenSessionCommand command);

    CashSession closeSession(UUID clinicId, UUID actingUserId, CloseSessionCommand command);

    Optional<CashSession> getCurrentSession(UUID clinicId, UUID actingUserId);

    CashSession getSession(UUID sessionId, UUID clinicId, UUID actingUserId);

    List<CashSession> listSessions(UUID clinicId, UUID actingUserId);

    record OpenSessionCommand(UUID openedByStaffId, BigDecimal openingAmount) {}

    record CloseSessionCommand(UUID closedByStaffId, BigDecimal countedCashAmount) {}
}
