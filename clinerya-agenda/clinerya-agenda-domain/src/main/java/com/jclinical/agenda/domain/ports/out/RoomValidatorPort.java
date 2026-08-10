package com.jclinical.agenda.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface RoomValidatorPort {

    Optional<RoomSnapshot> findActiveRoom(UUID roomId, UUID clinicId);

    record RoomSnapshot(UUID roomId, UUID clinicId, String name) {}
}
