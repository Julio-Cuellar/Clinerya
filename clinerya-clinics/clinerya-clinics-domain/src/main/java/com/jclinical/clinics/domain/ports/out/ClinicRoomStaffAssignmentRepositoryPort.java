package com.jclinical.clinics.domain.ports.out;

import com.jclinical.clinics.domain.model.ClinicRoomStaffAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicRoomStaffAssignmentRepositoryPort {
    ClinicRoomStaffAssignment save(ClinicRoomStaffAssignment assignment);
    Optional<ClinicRoomStaffAssignment> findByClinicIdAndRoomIdAndStaffId(UUID clinicId, UUID roomId, UUID staffId);
    List<ClinicRoomStaffAssignment> findByClinicIdAndRoomId(UUID clinicId, UUID roomId);
}
