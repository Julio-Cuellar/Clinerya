package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.RoomBlock;
import com.jclinical.agenda.domain.model.RoomBlockType;
import com.jclinical.agenda.domain.ports.in.ManageRoomBlocksUseCase.CreateRoomBlockCommand;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.RoomBlockRepositoryPort;
import com.jclinical.agenda.domain.ports.out.RoomValidatorPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomBlockServiceTest {

    @Mock
    private RoomBlockRepositoryPort roomBlockRepository;
    @Mock
    private AppointmentRepositoryPort appointmentRepository;
    @Mock
    private RoomValidatorPort roomValidator;

    private final StaffPermissionCheckerPort permissionChecker = (clinicId, userId, permission) -> true;
    private final UUID actingUserId = UUID.randomUUID();
    private RoomBlockService service;

    @BeforeEach
    void setUp() {
        service = new RoomBlockService(roomBlockRepository, appointmentRepository, roomValidator, permissionChecker);
    }

    @Test
    void createBlockRejectsAnAppointmentOverlap() {
        UUID clinicId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        LocalDateTime startsAt = LocalDateTime.of(2026, 8, 10, 10, 0);
        LocalDateTime endsAt = startsAt.plusHours(1);

        when(roomValidator.findActiveRoom(roomId, clinicId))
                .thenReturn(Optional.of(new RoomValidatorPort.RoomSnapshot(roomId, clinicId, "Consultorio 1")));
        when(roomBlockRepository.existsOverlappingActiveByRoom(roomId, clinicId, startsAt, endsAt)).thenReturn(false);
        when(appointmentRepository.existsOverlappingAppointmentByRoom(roomId, clinicId, startsAt, endsAt, null)).thenReturn(true);

        CreateRoomBlockCommand command =
                new CreateRoomBlockCommand(roomId, startsAt, endsAt, RoomBlockType.MAINTENANCE, "Cambio de equipo", null);
        assertThrows(IllegalStateException.class, () -> service.createBlock(clinicId, actingUserId, command));
    }

    @Test
    void createBlockSavesAnActiveBlock() {
        UUID clinicId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        LocalDateTime startsAt = LocalDateTime.of(2026, 8, 10, 10, 0);
        LocalDateTime endsAt = startsAt.plusHours(1);

        when(roomValidator.findActiveRoom(roomId, clinicId))
                .thenReturn(Optional.of(new RoomValidatorPort.RoomSnapshot(roomId, clinicId, "Consultorio 1")));
        when(roomBlockRepository.existsOverlappingActiveByRoom(roomId, clinicId, startsAt, endsAt)).thenReturn(false);
        when(appointmentRepository.existsOverlappingAppointmentByRoom(roomId, clinicId, startsAt, endsAt, null)).thenReturn(false);
        when(roomBlockRepository.save(any(RoomBlock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomBlock created = service.createBlock(clinicId, actingUserId,
                new CreateRoomBlockCommand(roomId, startsAt, endsAt, RoomBlockType.CLEANING, "Limpieza profunda", null));

        assertEquals(roomId, created.getRoomId());
        assertEquals(RoomBlockType.CLEANING, created.getType());
        assertEquals("Limpieza profunda", created.getReason());
    }
}
