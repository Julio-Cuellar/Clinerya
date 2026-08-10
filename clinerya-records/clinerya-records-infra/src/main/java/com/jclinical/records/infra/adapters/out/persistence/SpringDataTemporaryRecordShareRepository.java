package com.jclinical.records.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataTemporaryRecordShareRepository extends JpaRepository<TemporaryRecordShareEntity, UUID> {
    Optional<TemporaryRecordShareEntity> findByToken(String token);
}
