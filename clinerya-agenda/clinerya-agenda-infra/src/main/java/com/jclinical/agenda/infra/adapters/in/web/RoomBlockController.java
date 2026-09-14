package com.jclinical.agenda.infra.adapters.in.web;

import com.jclinical.agenda.domain.model.RoomBlock;
import com.jclinical.agenda.domain.ports.in.ManageRoomBlocksUseCase;
import com.jclinical.agenda.domain.ports.in.ManageRoomBlocksUseCase.CreateRoomBlockCommand;
import com.jclinical.agenda.infra.adapters.in.web.dto.CreateRoomBlockRequest;
import com.jclinical.agenda.infra.adapters.in.web.dto.RoomBlockResponse;
import com.jclinical.agenda.infra.adapters.out.persistence.RoomBlockMapper;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/room-blocks")
@RequiredArgsConstructor
public class RoomBlockController {

    private final ManageRoomBlocksUseCase roomBlocksUseCase;
    private final RoomBlockMapper mapper;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<RoomBlockResponse> create(
            @PathVariable UUID clinicId,
            @RequestBody CreateRoomBlockRequest request) {
        RoomBlock block = roomBlocksUseCase.createBlock(
                currentUserResolver.getCurrentUserId(), clinicId, new CreateRoomBlockCommand(
                request.roomId(),
                request.startsAt(),
                request.endsAt(),
                request.type(),
                request.reason(),
                null
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(block));
    }

    @GetMapping
    public ResponseEntity<List<RoomBlockResponse>> list(
            @PathVariable UUID clinicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(roomBlocksUseCase
                .listByClinicRange(currentUserResolver.getCurrentUserId(), clinicId, from, to).stream()
                .map(mapper::toResponse)
                .toList());
    }

    @PatchMapping("/{blockId}/deactivate")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID clinicId,
            @PathVariable UUID blockId) {
        roomBlocksUseCase.deactivateBlock(currentUserResolver.getCurrentUserId(), clinicId, blockId);
        return ResponseEntity.noContent().build();
    }
}
