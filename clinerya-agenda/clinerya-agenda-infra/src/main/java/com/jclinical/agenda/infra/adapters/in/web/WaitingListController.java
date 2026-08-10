package com.jclinical.agenda.infra.adapters.in.web;

import com.jclinical.agenda.domain.model.WaitingListEntry;
import com.jclinical.agenda.domain.ports.in.ManageWaitingListUseCase;
import com.jclinical.agenda.infra.adapters.in.web.dto.AddToWaitingListRequest;
import com.jclinical.agenda.infra.adapters.in.web.dto.WaitingListResponse;
import com.jclinical.agenda.infra.adapters.out.persistence.WaitingListMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clinics/{clinicId}/waiting-list")
@RequiredArgsConstructor
public class WaitingListController {

    private final ManageWaitingListUseCase manageWaitingListUseCase;
    private final WaitingListMapper waitingListMapper;

    @PostMapping
    public ResponseEntity<WaitingListResponse> addToWaitingList(
            @PathVariable UUID clinicId,
            @RequestBody AddToWaitingListRequest request) {
        WaitingListEntry entry = manageWaitingListUseCase.addToWaitingList(
                clinicId,
                request.patientId(),
                request.doctorStaffId(),
                request.roomId(),
                request.preferredDateFrom(),
                request.preferredDateTo(),
                request.preferredTimeRange(),
                request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(waitingListMapper.toResponse(entry));
    }

    @GetMapping
    public ResponseEntity<List<WaitingListResponse>> getWaitingList(
            @PathVariable UUID clinicId,
            @RequestParam(required = false, defaultValue = "true") boolean waitingOnly) {
        List<WaitingListEntry> list = manageWaitingListUseCase.listWaitingList(clinicId, waitingOnly);
        List<WaitingListResponse> response = list.stream()
                .map(waitingListMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{entryId}/status")
    public ResponseEntity<WaitingListResponse> updateStatus(
            @PathVariable UUID clinicId,
            @PathVariable UUID entryId,
            @RequestParam WaitingListEntry.Status status) {
        WaitingListEntry entry = manageWaitingListUseCase.updateStatus(clinicId, entryId, status);
        return ResponseEntity.ok(waitingListMapper.toResponse(entry));
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> removeFromWaitingList(
            @PathVariable UUID clinicId,
            @PathVariable UUID entryId) {
        manageWaitingListUseCase.removeFromWaitingList(clinicId, entryId);
        return ResponseEntity.noContent().build();
    }
}
