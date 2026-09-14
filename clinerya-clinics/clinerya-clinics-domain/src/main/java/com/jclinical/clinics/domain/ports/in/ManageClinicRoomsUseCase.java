package com.jclinical.clinics.domain.ports.in;

import com.jclinical.clinics.domain.model.ClinicRoom;
import com.jclinical.clinics.domain.model.ClinicRoomStaffAssignment;

import java.util.List;
import java.util.UUID;

public interface ManageClinicRoomsUseCase {
    ClinicRoom createRoom(UUID actingUserId, UUID clinicId, String name, String code, String colorHex, String description);
    ClinicRoom updateRoom(UUID actingUserId, UUID clinicId, UUID roomId, String name, String code, String colorHex, String description);
    void deactivateRoom(UUID actingUserId, UUID clinicId, UUID roomId);
    void activateRoom(UUID actingUserId, UUID clinicId, UUID roomId);
    List<ClinicRoom> getRoomsByClinic(UUID actingUserId, UUID clinicId);

    /** Ruta interna (validacion de consultorio al agendar): sin control de permiso. */
    default List<ClinicRoom> getActiveRoomsByClinic(UUID clinicId) {
        return getActiveRoomsByClinic(null, clinicId);
    }

    List<ClinicRoom> getActiveRoomsByClinic(UUID actingUserId, UUID clinicId);
    List<ClinicRoomStaffAssignment> getStaffAssignments(UUID actingUserId, UUID clinicId, UUID roomId);
    ClinicRoomStaffAssignment assignStaff(UUID actingUserId, UUID clinicId, UUID roomId, UUID staffId);
    void unassignStaff(UUID actingUserId, UUID clinicId, UUID roomId, UUID staffId);
}
