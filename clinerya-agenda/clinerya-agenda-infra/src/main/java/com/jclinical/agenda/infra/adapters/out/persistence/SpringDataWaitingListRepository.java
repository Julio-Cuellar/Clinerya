package com.jclinical.agenda.infra.adapters.out.persistence;

import com.jclinical.agenda.domain.model.WaitingListEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataWaitingListRepository extends JpaRepository<WaitingListEntity, UUID> {
    Optional<WaitingListEntity> findByIdAndClinicId(UUID id, UUID clinicId);
    List<WaitingListEntity> findByClinicIdAndStatusOrderByCreatedAtDesc(UUID clinicId, WaitingListEntry.Status status);
    List<WaitingListEntity> findByClinicIdOrderByCreatedAtDesc(UUID clinicId);
}
