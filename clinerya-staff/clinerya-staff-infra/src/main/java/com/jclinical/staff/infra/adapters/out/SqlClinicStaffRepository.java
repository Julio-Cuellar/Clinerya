package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SqlClinicStaffRepository implements ClinicStaffRepositoryPort {

    private final SpringDataClinicStaffRepository springDataRepository;
    private final ClinicStaffMapper mapper;

    @Override
    public ClinicStaff save(ClinicStaff staff) {
        ClinicStaffEntity entity = mapper.toEntity(staff);
        ClinicStaffEntity savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ClinicStaff> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ClinicStaff> findByClinicIdAndUserId(UUID clinicId, UUID userId) {
        return springDataRepository.findByClinicIdAndUserId(clinicId, userId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ClinicStaff> findByClinicId(UUID clinicId) {
        return springDataRepository.findByClinicId(clinicId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClinicStaff> findByUserId(UUID userId) {
        return springDataRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
