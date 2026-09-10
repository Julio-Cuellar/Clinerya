package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.RoomBlock;
import com.jclinical.agenda.domain.model.RoomBlockType;
import com.jclinical.agenda.domain.ports.in.ManageRoomBlocksUseCase;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.RoomBlockRepositoryPort;
import com.jclinical.agenda.domain.ports.out.RoomValidatorPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class RoomBlockService implements ManageRoomBlocksUseCase {

    private final RoomBlockRepositoryPort roomBlockRepository;
    private final AppointmentRepositoryPort appointmentRepository;
    private final RoomValidatorPort roomValidator;
    private final StaffPermissionCheckerPort permissionChecker;

    public RoomBlockService(
            RoomBlockRepositoryPort roomBlockRepository,
            AppointmentRepositoryPort appointmentRepository,
            RoomValidatorPort roomValidator,
            StaffPermissionCheckerPort permissionChecker) {
        this.roomBlockRepository = roomBlockRepository;
        this.appointmentRepository = appointmentRepository;
        this.roomValidator = roomValidator;
        this.permissionChecker = permissionChecker;
    }

    private void authorize(UUID actingUserId, UUID clinicId, StaffPermission permission) {
        if (actingUserId == null || !permissionChecker.hasPermission(clinicId, actingUserId, permission)) {
            throw new ClinicAccessDeniedException("No tienes permisos para esta operación de agenda.");
        }
    }

    @Override
    public RoomBlock createBlock(UUID clinicId, UUID actingUserId, CreateRoomBlockCommand command) {
        authorize(actingUserId, clinicId, StaffPermission.MANAGE_ROOMS);
        validateTimeRange(command.startsAt(), command.endsAt());
        RoomBlockType type = command.type() != null ? command.type() : RoomBlockType.OTHER;

        roomValidator.findActiveRoom(command.roomId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El consultorio no existe o está inactivo en esta clínica."));

        if (roomBlockRepository.existsOverlappingActiveByRoom(
                command.roomId(), clinicId, command.startsAt(), command.endsAt())) {
            throw new IllegalStateException("El consultorio ya tiene otro bloqueo activo en ese horario.");
        }

        if (appointmentRepository.existsOverlappingAppointmentByRoom(
                command.roomId(), clinicId, command.startsAt(), command.endsAt(), null)) {
            throw new IllegalStateException("El consultorio tiene una cita activa en ese horario.");
        }

        LocalDateTime now = LocalDateTime.now();
        RoomBlock block = RoomBlock.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .roomId(command.roomId())
                .startsAt(command.startsAt())
                .endsAt(command.endsAt())
                .type(type)
                .reason(normalizeReason(command.reason()))
                .createdByUserId(command.createdByUserId())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return roomBlockRepository.save(block);
    }

    @Override
    public List<RoomBlock> listByClinicRange(UUID clinicId, UUID actingUserId, LocalDateTime from, LocalDateTime to) {
        authorize(actingUserId, clinicId, StaffPermission.VIEW_ROOMS);
        validateTimeRange(from, to);
        return roomBlockRepository.findByClinicIdAndRange(clinicId, from, to);
    }

    @Override
    public void deactivateBlock(UUID clinicId, UUID blockId, UUID actingUserId) {
        authorize(actingUserId, clinicId, StaffPermission.MANAGE_ROOMS);
        RoomBlock block = roomBlockRepository.findByIdAndClinicId(blockId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El bloqueo no existe en esta clínica."));
        block.deactivate();
        roomBlockRepository.save(block);
    }

    private void validateTimeRange(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("La fecha y hora de inicio deben ser anteriores a las de fin.");
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return null;
        }
        return reason.trim();
    }
}
