package com.jclinical.agenda.domain.ports.in;

import com.jclinical.agenda.domain.model.RoomBlock;
import com.jclinical.agenda.domain.model.RoomBlockType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageRoomBlocksUseCase {

    RoomBlock createBlock(UUID clinicId, UUID actingUserId, CreateRoomBlockCommand command);

    List<RoomBlock> listByClinicRange(UUID clinicId, UUID actingUserId, LocalDateTime from, LocalDateTime to);

    void deactivateBlock(UUID clinicId, UUID blockId, UUID actingUserId);

    record CreateRoomBlockCommand(
            UUID roomId,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            RoomBlockType type,
            String reason,
            UUID createdByUserId
    ) {}
}
