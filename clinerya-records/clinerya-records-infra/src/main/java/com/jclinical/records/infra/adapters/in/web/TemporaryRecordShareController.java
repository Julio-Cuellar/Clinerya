package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.TemporaryRecordShare;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase.SharedRecordSummary;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TemporaryRecordShareController {

    private final ManageTemporaryShareUseCase temporaryShareUseCase;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/api/v1/clinics/{clinicId}/patients/{patientId}/temporary-shares")
    public ResponseEntity<TemporaryRecordShareResponse> createShareLink(
            @PathVariable UUID clinicId,
            @PathVariable UUID patientId,
            @RequestBody CreateShareRequest request) {
        
        UUID requestingUserId = currentUserResolver.getCurrentUserId();
        int days = request.daysValid() > 0 ? request.daysValid() : 7;
        
        TemporaryRecordShare share = temporaryShareUseCase.createShareLink(
                clinicId, patientId, request.email(), days, requestingUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(new TemporaryRecordShareResponse(
                share.getId(),
                share.getClinicId(),
                share.getPatientId(),
                share.getEmail(),
                share.getToken(),
                share.getExpiresAt().toString()
        ));
    }

    @GetMapping("/api/v1/public/shared-history")
    public ResponseEntity<SharedRecordSummary> getSharedRecord(@RequestParam String token) {
        SharedRecordSummary summary = temporaryShareUseCase.getSharedRecord(token);
        return ResponseEntity.ok(summary);
    }

    public record CreateShareRequest(
            String email,
            int daysValid
    ) {}

    public record TemporaryRecordShareResponse(
            UUID id,
            UUID clinicId,
            UUID patientId,
            String email,
            String token,
            String expiresAt
    ) {}
}
