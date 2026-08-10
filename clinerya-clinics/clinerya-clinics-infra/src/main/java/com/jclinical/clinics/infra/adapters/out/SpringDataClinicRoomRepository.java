package com.jclinical.clinics.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataClinicRoomRepository extends JpaRepository<ClinicRoomEntity, UUID> {
    Optional<ClinicRoomEntity> findByIdAndClinicId(UUID id, UUID clinicId);
    List<ClinicRoomEntity> findByClinicIdOrderByNameAsc(UUID clinicId);
    List<ClinicRoomEntity> findByClinicIdAndActiveTrueOrderByNameAsc(UUID clinicId);

    @Query("SELECT COUNT(r) > 0 FROM ClinicRoomEntity r WHERE r.clinicId = :clinicId AND LOWER(r.name) = LOWER(:name) AND (:excludeId IS NULL OR r.id != :excludeId)")
    boolean existsByNameAndClinicId(@Param("name") String name, @Param("clinicId") UUID clinicId, @Param("excludeId") UUID excludeId);
}
