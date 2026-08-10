package com.jclinical.clinics.infra.adapters.out;

import com.jclinical.clinics.domain.model.ClinicRoomStaffAssignment;
public interface ClinicRoomStaffAssignmentMapper {
    ClinicRoomStaffAssignmentEntity toEntity(ClinicRoomStaffAssignment domain);
    ClinicRoomStaffAssignment toDomain(ClinicRoomStaffAssignmentEntity entity);
}
