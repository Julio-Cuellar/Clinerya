package com.jclinical.agenda.domain.ports.out;

import com.jclinical.agenda.domain.model.RoomBlock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomBlockRepositoryPort {

    RoomBlock save(RoomBlock block);

    Optional<RoomBlock> findByIdAndClinicId(UUID blockId, UUID clinicId);

    List<RoomBlock> findByClinicIdAndRange(UUID clinicId, LocalDateTime from, LocalDateTime to);

    boolean existsOverlappingActiveByRoom(UUID roomId, UUID clinicId, LocalDateTime startsAt, LocalDateTime endsAt);
}
