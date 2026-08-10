package com.jclinical.clinics.infra.adapters.out;

import com.jclinical.clinics.domain.model.ClinicRoomStaffAssignment;
import com.jclinical.clinics.domain.ports.out.ClinicRoomStaffAssignmentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SqlClinicRoomStaffAssignmentRepository implements ClinicRoomStaffAssignmentRepositoryPort {

    private final SpringDataClinicRoomStaffAssignmentRepository springDataRepository;
    private final ClinicRoomStaffAssignmentMapper mapper;

    @Override
    public ClinicRoomStaffAssignment save(ClinicRoomStaffAssignment assignment) {
        return mapper.toDomain(springDataRepository.save(mapper.toEntity(assignment)));
    }

    @Override
    public Optional<ClinicRoomStaffAssignment> findByClinicIdAndRoomIdAndStaffId(UUID clinicId, UUID roomId, UUID staffId) {
        return springDataRepository.findByClinicIdAndRoomIdAndStaffId(clinicId, roomId, staffId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ClinicRoomStaffAssignment> findByClinicIdAndRoomId(UUID clinicId, UUID roomId) {
        return springDataRepository.findByClinicIdAndRoomIdAndActiveTrueOrderByAssignedAtAsc(clinicId, roomId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
