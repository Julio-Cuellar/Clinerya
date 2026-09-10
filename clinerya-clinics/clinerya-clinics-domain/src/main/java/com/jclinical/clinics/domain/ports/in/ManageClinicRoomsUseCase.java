package com.jclinical.clinics.domain.ports.in;

import com.jclinical.clinics.domain.model.ClinicRoom;
import com.jclinical.clinics.domain.model.ClinicRoomStaffAssignment;

import java.util.List;
import java.util.UUID;

public interface ManageClinicRoomsUseCase {
    ClinicRoom createRoom(UUID clinicId, UUID actingUserId, String name, String code, String colorHex, String description);
    ClinicRoom updateRoom(UUID clinicId, UUID roomId, UUID actingUserId, String name, String code, String colorHex, String description);
    void deactivateRoom(UUID clinicId, UUID roomId, UUID actingUserId);
    void activateRoom(UUID clinicId, UUID roomId, UUID actingUserId);
    List<ClinicRoom> getRoomsByClinic(UUID clinicId, UUID actingUserId);
    List<ClinicRoom> getActiveRoomsByClinic(UUID clinicId, UUID actingUserId);

    /**
     * Lectura interna para el modulo de agenda, que valida existencia de consultorios al
     * crear o reagendar citas: no hay usuario en la peticion. No exponer desde un
     * controlador.
     */
    List<ClinicRoom> getActiveRoomsByClinicForSystem(UUID clinicId);

    List<ClinicRoomStaffAssignment> getStaffAssignments(UUID clinicId, UUID roomId, UUID actingUserId);
    ClinicRoomStaffAssignment assignStaff(UUID clinicId, UUID roomId, UUID staffId, UUID actingUserId);
    void unassignStaff(UUID clinicId, UUID roomId, UUID staffId, UUID actingUserId);
}
