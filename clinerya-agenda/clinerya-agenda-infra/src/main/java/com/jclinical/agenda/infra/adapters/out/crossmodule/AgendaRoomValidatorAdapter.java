package com.jclinical.agenda.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.out.RoomValidatorPort;
import com.jclinical.clinics.domain.model.ClinicRoom;
import com.jclinical.clinics.domain.ports.in.ManageClinicRoomsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgendaRoomValidatorAdapter implements RoomValidatorPort {

    private final ManageClinicRoomsUseCase clinicRoomsUseCase;

    @Override
    public Optional<RoomSnapshot> findActiveRoom(UUID roomId, UUID clinicId) {
        return clinicRoomsUseCase.getActiveRoomsByClinicForSystem(clinicId).stream()
                .filter(room -> room.getId().equals(roomId))
                .map(this::toSnapshot)
                .findFirst();
    }

    private RoomSnapshot toSnapshot(ClinicRoom room) {
        return new RoomSnapshot(room.getId(), room.getClinicId(), room.getName());
    }
}
