package com.jclinical.cash.infra.adapters.in.web;

import com.jclinical.cash.domain.model.CashSession;
import com.jclinical.cash.domain.ports.in.ManageCashSessionUseCase;
import com.jclinical.cash.domain.ports.in.ManageCashSessionUseCase.CloseSessionCommand;
import com.jclinical.cash.domain.ports.in.ManageCashSessionUseCase.OpenSessionCommand;
import com.jclinical.cash.infra.adapters.in.web.dto.CashSessionResponse;
import com.jclinical.cash.infra.adapters.in.web.dto.CloseCashSessionRequest;
import com.jclinical.cash.infra.adapters.in.web.dto.OpenCashSessionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/cash-sessions")
@RequiredArgsConstructor
public class CashSessionController {

    private final ManageCashSessionUseCase cashSessionUseCase;

    @PostMapping("")
    public ResponseEntity<CashSessionResponse> openSession(
            @PathVariable UUID clinicId,
            @RequestBody OpenCashSessionRequest request) {
        OpenSessionCommand command = new OpenSessionCommand(request.openedByStaffId(), request.openingAmount());
        CashSession session = cashSessionUseCase.openSession(clinicId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(session));
    }

    @PostMapping("/close")
    public ResponseEntity<CashSessionResponse> closeSession(
            @PathVariable UUID clinicId,
            @RequestBody CloseCashSessionRequest request) {
        CloseSessionCommand command = new CloseSessionCommand(request.closedByStaffId(), request.countedCashAmount());
        CashSession session = cashSessionUseCase.closeSession(clinicId, command);
        return ResponseEntity.ok(toResponse(session));
    }

    @GetMapping("/current")
    public ResponseEntity<CashSessionResponse> getCurrentSession(@PathVariable UUID clinicId) {
        Optional<CashSession> session = cashSessionUseCase.getCurrentSession(clinicId);
        return session.map(value -> ResponseEntity.ok(toResponse(value)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("")
    public ResponseEntity<List<CashSessionResponse>> listSessions(@PathVariable UUID clinicId) {
        List<CashSessionResponse> responses = cashSessionUseCase.listSessions(clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<CashSessionResponse> getSession(
            @PathVariable UUID clinicId,
            @PathVariable UUID sessionId) {
        CashSession session = cashSessionUseCase.getSession(sessionId, clinicId);
        return ResponseEntity.ok(toResponse(session));
    }

    private CashSessionResponse toResponse(CashSession session) {
        return new CashSessionResponse(
                session.getId(),
                session.getClinicId(),
                session.getOpenedByStaffId(),
                session.getOpenedAt(),
                session.getOpeningAmount(),
                session.getClosedByStaffId(),
                session.getClosedAt(),
                session.getCountedCashAmount(),
                session.getExpectedCashAmount(),
                session.getCashDifference(),
                session.getStatus()
        );
    }
}
