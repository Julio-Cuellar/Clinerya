package com.jclinical.clinics.infra.adapters.out;

import com.jclinical.clinics.domain.model.ClinicRoom;
import com.jclinical.clinics.domain.ports.out.ClinicRoomRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SqlClinicRoomRepository implements ClinicRoomRepositoryPort {

    private final SpringDataClinicRoomRepository springDataRepository;
    private final ClinicRoomMapper roomMapper;

    @Override
    public ClinicRoom save(ClinicRoom room) {
        ClinicRoomEntity entity = roomMapper.toEntity(room);
        ClinicRoomEntity saved = springDataRepository.save(entity);
        return roomMapper.toDomain(saved);
    }

    @Override
    public Optional<ClinicRoom> findByIdAndClinicId(UUID id, UUID clinicId) {
        return springDataRepository.findByIdAndClinicId(id, clinicId)
                .map(roomMapper::toDomain);
    }

    @Override
    public List<ClinicRoom> findByClinicId(UUID clinicId) {
        return springDataRepository.findByClinicIdOrderByNameAsc(clinicId).stream()
                .map(roomMapper::toDomain)
                .toList();
    }

    @Override
    public List<ClinicRoom> findActiveByClinicId(UUID clinicId) {
        return springDataRepository.findByClinicIdAndActiveTrueOrderByNameAsc(clinicId).stream()
                .map(roomMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNameAndClinicId(String name, UUID clinicId, UUID excludeId) {
        return springDataRepository.existsByNameAndClinicId(name, clinicId, excludeId);
    }
}
