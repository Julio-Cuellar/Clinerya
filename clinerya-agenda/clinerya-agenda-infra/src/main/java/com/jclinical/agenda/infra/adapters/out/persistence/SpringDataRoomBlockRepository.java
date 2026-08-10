package com.jclinical.agenda.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataRoomBlockRepository extends JpaRepository<RoomBlockEntity, UUID> {

    Optional<RoomBlockEntity> findByIdAndClinicId(UUID id, UUID clinicId);

    @Query("select b from RoomBlockEntity b where b.clinicId = :clinicId "
            + "and b.startsAt < :to and b.endsAt > :from "
            + "order by b.startsAt asc")
    List<RoomBlockEntity> findByClinicIdAndRange(
            @Param("clinicId") UUID clinicId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("select count(b) > 0 from RoomBlockEntity b where b.roomId = :roomId and b.clinicId = :clinicId "
            + "and b.active = true and b.startsAt < :endsAt and b.endsAt > :startsAt")
    boolean existsOverlappingActiveByRoom(
            @Param("roomId") UUID roomId,
            @Param("clinicId") UUID clinicId,
            @Param("startsAt") LocalDateTime startsAt,
            @Param("endsAt") LocalDateTime endsAt);
}
