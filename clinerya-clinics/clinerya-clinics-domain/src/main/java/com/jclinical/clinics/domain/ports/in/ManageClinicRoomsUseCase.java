package com.jclinical.clinics.domain.ports.in;

import com.jclinical.clinics.domain.model.ClinicRoom;
import com.jclinical.clinics.domain.model.ClinicRoomStaffAssignment;

import java.util.List;
import java.util.UUID;

public interface ManageClinicRoomsUseCase {
    ClinicRoom createRoom(UUID clinicId, String name, String code, String colorHex, String description);
    ClinicRoom updateRoom(UUID clinicId, UUID roomId, String name, String code, String colorHex, String description);
    void deactivateRoom(UUID clinicId, UUID roomId);
    void activateRoom(UUID clinicId, UUID roomId);
    List<ClinicRoom> getRoomsByClinic(UUID clinicId);
    List<ClinicRoom> getActiveRoomsByClinic(UUID clinicId);
    List<ClinicRoomStaffAssignment> getStaffAssignments(UUID clinicId, UUID roomId);
    ClinicRoomStaffAssignment assignStaff(UUID clinicId, UUID roomId, UUID staffId);
    void unassignStaff(UUID clinicId, UUID roomId, UUID staffId);
}
