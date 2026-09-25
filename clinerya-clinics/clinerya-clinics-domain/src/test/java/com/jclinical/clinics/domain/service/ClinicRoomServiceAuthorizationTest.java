package com.jclinical.clinics.domain.service;

import com.jclinical.clinics.domain.model.ClinicRoom;
import com.jclinical.clinics.domain.model.ClinicRoomStaffAssignment;
import com.jclinical.clinics.domain.ports.out.ClinicRoomRepositoryPort;
import com.jclinical.clinics.domain.ports.out.ClinicRoomStaffAssignmentRepositoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
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
    private final InMemoryAssignmentRepository assignmentRepository = new InMemoryAssignmentRepository();
    private final InMemoryStaffRepository staffRepository = new InMemoryStaffRepository();
    private final UUID clinicId = UUID.randomUUID();
    private final UUID actingUserId = UUID.randomUUID();
    private StaffPermission grantedPermission;
    private final StaffPermissionCheckerPort permissionChecker =
            (clinic, user, permission) -> permission == grantedPermission;

    private ClinicRoomService service;

    @BeforeEach
    void setUp() {
        service = new ClinicRoomService(roomRepository, assignmentRepository, staffRepository, permissionChecker);
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

    @Test
    void assignsAClinicAdminWhoAttendsPatientsToARoom() {
        grantedPermission = StaffPermission.ASSIGN_ROOM_STAFF;
        UUID roomId = activeRoom();
        ClinicStaff owner = staff(StaffRole.CLINIC_ADMIN, true);

        ClinicRoomStaffAssignment assignment = service.assignStaff(actingUserId, clinicId, roomId, owner.getId());

        assertEquals(owner.getId(), assignment.getStaffId());
    }

    @Test
    void rejectsAssigningStaffWhoDoesNotAttendPatients() {
        grantedPermission = StaffPermission.ASSIGN_ROOM_STAFF;
        UUID roomId = activeRoom();
        ClinicStaff administrator = staff(StaffRole.CLINIC_ADMIN, false);

        assertThrows(IllegalArgumentException.class,
                () -> service.assignStaff(actingUserId, clinicId, roomId, administrator.getId()));
        assertTrue(assignmentRepository.items.isEmpty());
    }

    private UUID activeRoom() {
        UUID roomId = UUID.randomUUID();
        roomRepository.items.add(ClinicRoom.builder().id(roomId).clinicId(clinicId).name("Consultorio 1").active(true).build());
        return roomId;
    }

    private ClinicStaff staff(StaffRole role, boolean attendsPatients) {
        ClinicStaff staff = ClinicStaff.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .userId(UUID.randomUUID())
                .role(role)
                .active(true)
                .attendsPatients(attendsPatients)
                .build();
        staffRepository.items.add(staff);
        return staff;
    }

    private static final class InMemoryAssignmentRepository implements ClinicRoomStaffAssignmentRepositoryPort {
        private final List<ClinicRoomStaffAssignment> items = new ArrayList<>();

        @Override
        public ClinicRoomStaffAssignment save(ClinicRoomStaffAssignment assignment) {
            items.removeIf(existing -> existing.getId().equals(assignment.getId()));
            items.add(assignment);
            return assignment;
        }

        @Override
        public Optional<ClinicRoomStaffAssignment> findByClinicIdAndRoomIdAndStaffId(UUID clinicId, UUID roomId, UUID staffId) {
            return items.stream()
                    .filter(item -> item.getClinicId().equals(clinicId) && item.getRoomId().equals(roomId)
                            && item.getStaffId().equals(staffId))
                    .findFirst();
        }

        @Override
        public List<ClinicRoomStaffAssignment> findByClinicIdAndRoomId(UUID clinicId, UUID roomId) {
            return items.stream()
                    .filter(item -> item.getClinicId().equals(clinicId) && item.getRoomId().equals(roomId))
                    .toList();
        }
    }

    private static final class InMemoryStaffRepository implements ClinicStaffRepositoryPort {
        private final List<ClinicStaff> items = new ArrayList<>();

        @Override
        public ClinicStaff save(ClinicStaff staff) {
            items.removeIf(existing -> existing.getId().equals(staff.getId()));
            items.add(staff);
            return staff;
        }

        @Override
        public Optional<ClinicStaff> findById(UUID id) {
            return items.stream().filter(item -> item.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<ClinicStaff> findByClinicIdAndUserId(UUID clinicId, UUID userId) {
            return items.stream()
                    .filter(item -> item.getClinicId().equals(clinicId) && item.getUserId().equals(userId))
                    .findFirst();
        }

        @Override
        public List<ClinicStaff> findByClinicId(UUID clinicId) {
            return items.stream().filter(item -> item.getClinicId().equals(clinicId)).toList();
        }

        @Override
        public List<ClinicStaff> findByUserId(UUID userId) {
            return items.stream().filter(item -> item.getUserId().equals(userId)).toList();
        }
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
