package com.jclinical.clinics.domain.service;

import com.jclinical.clinics.domain.model.ClinicRoom;
import com.jclinical.clinics.domain.model.ClinicRoomStaffAssignment;
import com.jclinical.clinics.domain.ports.in.ManageClinicRoomsUseCase;
import com.jclinical.clinics.domain.ports.out.ClinicRoomStaffAssignmentRepositoryPort;
import com.jclinical.clinics.domain.ports.out.ClinicRoomRepositoryPort;
import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ClinicRoomService implements ManageClinicRoomsUseCase {

    private final ClinicRoomRepositoryPort roomRepository;
    private final ClinicRoomStaffAssignmentRepositoryPort assignmentRepository;
    private final ClinicStaffRepositoryPort clinicStaffRepository;
    private final StaffPermissionCheckerPort permissionChecker;

    public ClinicRoomService(ClinicRoomRepositoryPort roomRepository,
                             ClinicRoomStaffAssignmentRepositoryPort assignmentRepository,
                             ClinicStaffRepositoryPort clinicStaffRepository,
                             StaffPermissionCheckerPort permissionChecker) {
        this.roomRepository = roomRepository;
        this.assignmentRepository = assignmentRepository;
        this.clinicStaffRepository = clinicStaffRepository;
        this.permissionChecker = permissionChecker;
    }

    private void authorize(UUID actingUserId, UUID clinicId, StaffPermission permission) {
        if (actingUserId == null || !permissionChecker.hasPermission(clinicId, actingUserId, permission)) {
            throw new ClinicAccessDeniedException("No tienes permisos para esta operación de la clínica.");
        }
    }

    @Override
    public ClinicRoom createRoom(UUID clinicId, UUID actingUserId, String name, String code, String colorHex, String description) {
        authorize(actingUserId, clinicId, StaffPermission.CREATE_ROOMS);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del consultorio o sillón es obligatorio.");
        }
        if (roomRepository.existsByNameAndClinicId(name.trim(), clinicId, null)) {
            throw new IllegalArgumentException("Ya existe un consultorio o sillón con ese nombre en esta clínica.");
        }

        ClinicRoom room = ClinicRoom.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .name(name.trim())
                .code(code != null ? code.trim() : null)
                .colorHex(colorHex != null ? colorHex.trim() : "#3B82F6")
                .description(description != null ? description.trim() : null)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return roomRepository.save(room);
    }

    @Override
    public ClinicRoom updateRoom(UUID clinicId, UUID roomId, UUID actingUserId, String name, String code, String colorHex, String description) {
        authorize(actingUserId, clinicId, StaffPermission.EDIT_ROOMS);
        ClinicRoom room = roomRepository.findByIdAndClinicId(roomId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El consultorio especificado no existe."));

        if (name != null && !name.trim().isEmpty()) {
            if (roomRepository.existsByNameAndClinicId(name.trim(), clinicId, roomId)) {
                throw new IllegalArgumentException("Ya existe otro consultorio o sillón con ese nombre.");
            }
        }

        room.update(
                name != null ? name.trim() : room.getName(),
                code != null ? code.trim() : room.getCode(),
                colorHex != null ? colorHex.trim() : room.getColorHex(),
                description != null ? description.trim() : room.getDescription()
        );

        return roomRepository.save(room);
    }

    @Override
    public void deactivateRoom(UUID clinicId, UUID roomId, UUID actingUserId) {
        authorize(actingUserId, clinicId, StaffPermission.MANAGE_ROOMS);
        ClinicRoom room = roomRepository.findByIdAndClinicId(roomId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El consultorio especificado no existe."));
        room.deactivate();
        roomRepository.save(room);
    }

    @Override
    public void activateRoom(UUID clinicId, UUID roomId, UUID actingUserId) {
        authorize(actingUserId, clinicId, StaffPermission.MANAGE_ROOMS);
        ClinicRoom room = roomRepository.findByIdAndClinicId(roomId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El consultorio especificado no existe."));
        room.activate();
        roomRepository.save(room);
    }

    @Override
    public List<ClinicRoom> getRoomsByClinic(UUID clinicId, UUID actingUserId) {
        authorize(actingUserId, clinicId, StaffPermission.VIEW_ROOMS);
        return roomRepository.findByClinicId(clinicId);
    }

    @Override
    public List<ClinicRoom> getActiveRoomsByClinic(UUID clinicId, UUID actingUserId) {
        authorize(actingUserId, clinicId, StaffPermission.VIEW_ROOMS);
        return roomRepository.findActiveByClinicId(clinicId);
    }

    @Override
    public List<ClinicRoom> getActiveRoomsByClinicForSystem(UUID clinicId) {
        return roomRepository.findActiveByClinicId(clinicId);
    }

    @Override
    public List<ClinicRoomStaffAssignment> getStaffAssignments(UUID clinicId, UUID roomId, UUID actingUserId) {
        authorize(actingUserId, clinicId, StaffPermission.VIEW_ROOMS);
        ensureRoom(clinicId, roomId);
        return assignmentRepository.findByClinicIdAndRoomId(clinicId, roomId);
    }

    @Override
    public ClinicRoomStaffAssignment assignStaff(UUID clinicId, UUID roomId, UUID staffId, UUID actingUserId) {
        authorize(actingUserId, clinicId, StaffPermission.ASSIGN_ROOM_STAFF);
        ClinicRoom room = ensureRoom(clinicId, roomId);
        if (!room.isActive()) {
            throw new IllegalStateException("No se puede asignar personal a un consultorio inactivo.");
        }

        ClinicStaff staff = clinicStaffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("El miembro del personal no existe."));
        if (!clinicId.equals(staff.getClinicId()) || !staff.isActive()) {
            throw new IllegalArgumentException("El miembro del personal no pertenece a la clinica activa.");
        }
        if (staff.getRole() != StaffRole.DOCTOR) {
            throw new IllegalArgumentException("Solo se pueden asignar doctores a un consultorio.");
        }

        ClinicRoomStaffAssignment assignment = assignmentRepository
                .findByClinicIdAndRoomIdAndStaffId(clinicId, roomId, staffId)
                .orElseGet(() -> ClinicRoomStaffAssignment.builder()
                        .id(UUID.randomUUID())
                        .clinicId(clinicId)
                        .roomId(roomId)
                        .staffId(staffId)
                        .active(true)
                        .assignedAt(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
        if (!assignment.isActive()) {
            assignment.reactivate();
        }
        return assignmentRepository.save(assignment);
    }

    @Override
    public void unassignStaff(UUID clinicId, UUID roomId, UUID staffId, UUID actingUserId) {
        authorize(actingUserId, clinicId, StaffPermission.ASSIGN_ROOM_STAFF);
        ensureRoom(clinicId, roomId);
        ClinicRoomStaffAssignment assignment = assignmentRepository
                .findByClinicIdAndRoomIdAndStaffId(clinicId, roomId, staffId)
                .orElseThrow(() -> new IllegalArgumentException("La asignacion no existe."));
        if (assignment.isActive()) {
            assignment.deactivate();
            assignmentRepository.save(assignment);
        }
    }

    private ClinicRoom ensureRoom(UUID clinicId, UUID roomId) {
        return roomRepository.findByIdAndClinicId(roomId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El consultorio especificado no existe."));
    }
}
