package com.jclinical.agenda.domain.ports.in;

import com.jclinical.agenda.domain.model.RoomBlock;
import com.jclinical.agenda.domain.model.RoomBlockType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageRoomBlocksUseCase {

    RoomBlock createBlock(UUID clinicId, CreateRoomBlockCommand command);

    List<RoomBlock> listByClinicRange(UUID clinicId, LocalDateTime from, LocalDateTime to);

    void deactivateBlock(UUID clinicId, UUID blockId);

    record CreateRoomBlockCommand(
            UUID roomId,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            RoomBlockType type,
            String reason,
            UUID createdByUserId
    ) {}
}
