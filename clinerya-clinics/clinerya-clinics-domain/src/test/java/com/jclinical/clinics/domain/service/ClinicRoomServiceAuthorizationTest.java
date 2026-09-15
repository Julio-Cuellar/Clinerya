package com.jclinical.clinics.domain.service;

import com.jclinical.clinics.domain.model.ClinicRoom;
import com.jclinical.clinics.domain.ports.out.ClinicRoomRepositoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresion de P3: ClinicAccessInterceptor solo comprobaba pertenencia a la clinica,
 * asi que cualquier miembro activo podia dar de alta o consultar consultorios/sillones.
 * Ahora se exige CREATE_ROOMS/VIEW_ROOMS en el dominio.
 */
class ClinicRoomServiceAuthorizationTest {

    private final InMemoryRoomRepository roomRepository = new InMemoryRoomRepository();
    private final UUID clinicId = UUID.randomUUID();
    private final UUID actingUserId = UUID.randomUUID();
    private StaffPermission grantedPermission;
    private final StaffPermissionCheckerPort permissionChecker =
            (clinic, user, permission) -> permission == grantedPermission;

    private ClinicRoomService service;

    @BeforeEach
    void setUp() {
        service = new ClinicRoomService(roomRepository, null, null, permissionChecker);
    }

    @Test
    void deniesCreatingRoomWithoutCreatePermission() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.createRoom(actingUserId, clinicId, "Consultorio 1", null, null, null));
        assertTrue(roomRepository.items.isEmpty());
    }

    @Test
    void deniesListingRoomsWithoutViewPermission() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.getRoomsByClinic(actingUserId, clinicId));
    }

    @Test
    void deniesEveryOperationWhenActingUserIsNull() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.getRoomsByClinic(null, clinicId));
    }

    @Test
    void systemLookupBypassesPermissionCheck() {
        UUID roomId = UUID.randomUUID();
        roomRepository.items.add(ClinicRoom.builder().id(roomId).clinicId(clinicId).active(true).build());

        List<ClinicRoom> found = service.getActiveRoomsByClinic(clinicId);

        assertEquals(1, found.size());
    }

    @Test
    void allowsCreatingRoomWhenPermissionGranted() {
        grantedPermission = StaffPermission.CREATE_ROOMS;

        ClinicRoom created = service.createRoom(actingUserId, clinicId, "Consultorio 1", null, null, null);

        assertEquals(1, roomRepository.items.size());
        assertEquals(created.getId(), roomRepository.items.get(0).getId());
    }

    private static final class InMemoryRoomRepository implements ClinicRoomRepositoryPort {
        private final List<ClinicRoom> items = new ArrayList<>();

        @Override
        public ClinicRoom save(ClinicRoom room) {
            items.removeIf(existing -> existing.getId().equals(room.getId()));
            items.add(room);
            return room;
        }

        @Override
        public Optional<ClinicRoom> findByIdAndClinicId(UUID id, UUID clinicId) {
            return items.stream()
                    .filter(item -> item.getId().equals(id) && item.getClinicId().equals(clinicId))
                    .findFirst();
        }

        @Override
        public List<ClinicRoom> findByClinicId(UUID clinicId) {
            return items.stream().filter(item -> item.getClinicId().equals(clinicId)).toList();
        }

        @Override
        public List<ClinicRoom> findActiveByClinicId(UUID clinicId) {
            return items.stream()
                    .filter(item -> item.getClinicId().equals(clinicId))
                    .filter(ClinicRoom::isActive)
                    .toList();
        }

        @Override
        public boolean existsByNameAndClinicId(String name, UUID clinicId, UUID excludeId) {
            return items.stream()
                    .anyMatch(item -> item.getClinicId().equals(clinicId)
                            && item.getName().equalsIgnoreCase(name)
                            && !item.getId().equals(excludeId));
        }
    }
}
