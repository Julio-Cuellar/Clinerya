package com.jclinical.clinics.infra.adapters.in.web;

import com.jclinical.clinics.domain.model.ClinicRoom;
import com.jclinical.clinics.domain.ports.in.ManageClinicRoomsUseCase;
import com.jclinical.clinics.infra.adapters.in.web.dto.ClinicRoomResponse;
import com.jclinical.clinics.infra.adapters.in.web.dto.ClinicRoomStaffAssignmentResponse;
import com.jclinical.clinics.infra.adapters.in.web.dto.CreateClinicRoomRequest;
import com.jclinical.clinics.infra.adapters.in.web.dto.UpdateClinicRoomRequest;
import com.jclinical.clinics.infra.adapters.out.ClinicRoomMapper;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/rooms")
@RequiredArgsConstructor
public class ClinicRoomController {

    private final ManageClinicRoomsUseCase manageClinicRoomsUseCase;
    private final ClinicRoomMapper roomMapper;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<ClinicRoomResponse> createRoom(
            @PathVariable UUID clinicId,
            @RequestBody CreateClinicRoomRequest request) {
        ClinicRoom room = manageClinicRoomsUseCase.createRoom(
                currentUserResolver.getCurrentUserId(),
                clinicId,
                request.name(),
                request.code(),
                request.colorHex(),
                request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(roomMapper.toResponse(room));
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<ClinicRoomResponse> updateRoom(
            @PathVariable UUID clinicId,
            @PathVariable UUID roomId,
            @RequestBody UpdateClinicRoomRequest request) {
        ClinicRoom room = manageClinicRoomsUseCase.updateRoom(
                currentUserResolver.getCurrentUserId(),
                clinicId,
                roomId,
                request.name(),
                request.code(),
                request.colorHex(),
                request.description()
        );
        return ResponseEntity.ok(roomMapper.toResponse(room));
    }

    @GetMapping
    public ResponseEntity<List<ClinicRoomResponse>> getRooms(
            @PathVariable UUID clinicId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        UUID actingUserId = currentUserResolver.getCurrentUserId();
        List<ClinicRoom> rooms = activeOnly
                ? manageClinicRoomsUseCase.getActiveRoomsByClinic(actingUserId, clinicId)
                : manageClinicRoomsUseCase.getRoomsByClinic(actingUserId, clinicId);
        List<ClinicRoomResponse> response = rooms.stream()
                .map(roomMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{roomId}/deactivate")
    public ResponseEntity<Void> deactivateRoom(
            @PathVariable UUID clinicId,
            @PathVariable UUID roomId) {
        manageClinicRoomsUseCase.deactivateRoom(currentUserResolver.getCurrentUserId(), clinicId, roomId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{roomId}/activate")
    public ResponseEntity<Void> activateRoom(
            @PathVariable UUID clinicId,
            @PathVariable UUID roomId) {
        manageClinicRoomsUseCase.activateRoom(currentUserResolver.getCurrentUserId(), clinicId, roomId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/staff")
    public ResponseEntity<List<ClinicRoomStaffAssignmentResponse>> getAssignedStaff(
            @PathVariable UUID clinicId,
            @PathVariable UUID roomId) {
        return ResponseEntity.ok(manageClinicRoomsUseCase
                .getStaffAssignments(currentUserResolver.getCurrentUserId(), clinicId, roomId).stream()
                .map(assignment -> new ClinicRoomStaffAssignmentResponse(
                        assignment.getId(),
                        assignment.getClinicId(),
                        assignment.getRoomId(),
                        assignment.getStaffId(),
                        assignment.isActive(),
                        assignment.getAssignedAt(),
                        assignment.getCreatedAt(),
                        assignment.getUpdatedAt()))
                .toList());
    }

    @PutMapping("/{roomId}/staff/{staffId}")
    public ResponseEntity<ClinicRoomStaffAssignmentResponse> assignStaff(
            @PathVariable UUID clinicId,
            @PathVariable UUID roomId,
            @PathVariable UUID staffId) {
        var assignment = manageClinicRoomsUseCase.assignStaff(currentUserResolver.getCurrentUserId(), clinicId, roomId, staffId);
        return ResponseEntity.ok(new ClinicRoomStaffAssignmentResponse(
                assignment.getId(),
                assignment.getClinicId(),
                assignment.getRoomId(),
                assignment.getStaffId(),
                assignment.isActive(),
                assignment.getAssignedAt(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()));
    }

    @DeleteMapping("/{roomId}/staff/{staffId}")
    public ResponseEntity<Void> unassignStaff(
            @PathVariable UUID clinicId,
            @PathVariable UUID roomId,
            @PathVariable UUID staffId) {
        manageClinicRoomsUseCase.unassignStaff(currentUserResolver.getCurrentUserId(), clinicId, roomId, staffId);
        return ResponseEntity.noContent().build();
    }
}
