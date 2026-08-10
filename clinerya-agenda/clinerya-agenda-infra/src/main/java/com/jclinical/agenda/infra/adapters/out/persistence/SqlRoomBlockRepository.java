package com.jclinical.agenda.infra.adapters.out.persistence;

import com.jclinical.agenda.domain.model.RoomBlock;
import com.jclinical.agenda.domain.ports.out.RoomBlockRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlRoomBlockRepository implements RoomBlockRepositoryPort {

    private final SpringDataRoomBlockRepository springRepository;
    private final RoomBlockMapper mapper;

    @Override
    public RoomBlock save(RoomBlock block) {
        return mapper.toDomain(springRepository.save(mapper.toEntity(block)));
    }

    @Override
    public Optional<RoomBlock> findByIdAndClinicId(UUID blockId, UUID clinicId) {
        return springRepository.findByIdAndClinicId(blockId, clinicId).map(mapper::toDomain);
    }

    @Override
    public List<RoomBlock> findByClinicIdAndRange(UUID clinicId, LocalDateTime from, LocalDateTime to) {
        return springRepository.findByClinicIdAndRange(clinicId, from, to).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsOverlappingActiveByRoom(UUID roomId, UUID clinicId, LocalDateTime startsAt, LocalDateTime endsAt) {
        return springRepository.existsOverlappingActiveByRoom(roomId, clinicId, startsAt, endsAt);
    }
}
