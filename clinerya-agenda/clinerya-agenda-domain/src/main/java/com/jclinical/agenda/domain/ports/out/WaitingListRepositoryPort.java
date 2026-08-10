package com.jclinical.agenda.domain.ports.out;

import com.jclinical.agenda.domain.model.WaitingListEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaitingListRepositoryPort {
    WaitingListEntry save(WaitingListEntry entry);
    Optional<WaitingListEntry> findByIdAndClinicId(UUID id, UUID clinicId);
    List<WaitingListEntry> findByClinicIdAndStatus(UUID clinicId, WaitingListEntry.Status status);
    List<WaitingListEntry> findByClinicId(UUID clinicId);
    void delete(WaitingListEntry entry);
}
