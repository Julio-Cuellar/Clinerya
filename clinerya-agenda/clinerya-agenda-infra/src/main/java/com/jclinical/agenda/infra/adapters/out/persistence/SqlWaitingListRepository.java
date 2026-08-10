package com.jclinical.agenda.infra.adapters.out.persistence;

import com.jclinical.agenda.domain.model.WaitingListEntry;
import com.jclinical.agenda.domain.ports.out.WaitingListRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlWaitingListRepository implements WaitingListRepositoryPort {

    private final SpringDataWaitingListRepository springRepository;
    private final WaitingListMapper mapper;

    @Override
    public WaitingListEntry save(WaitingListEntry entry) {
        WaitingListEntity entity = mapper.toEntity(entry);
        WaitingListEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<WaitingListEntry> findByIdAndClinicId(UUID id, UUID clinicId) {
        return springRepository.findByIdAndClinicId(id, clinicId).map(mapper::toDomain);
    }

    @Override
    public List<WaitingListEntry> findByClinicIdAndStatus(UUID clinicId, WaitingListEntry.Status status) {
        return springRepository.findByClinicIdAndStatusOrderByCreatedAtDesc(clinicId, status).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<WaitingListEntry> findByClinicId(UUID clinicId) {
        return springRepository.findByClinicIdOrderByCreatedAtDesc(clinicId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(WaitingListEntry entry) {
        springRepository.deleteById(entry.getId());
    }
}
