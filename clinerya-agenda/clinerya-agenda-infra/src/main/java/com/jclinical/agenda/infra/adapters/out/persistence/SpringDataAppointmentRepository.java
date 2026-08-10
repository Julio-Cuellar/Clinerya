package com.jclinical.agenda.infra.adapters.out.persistence;

import com.jclinical.agenda.domain.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {

    Optional<AppointmentEntity> findByIdAndClinicId(UUID id, UUID clinicId);

    @Query("select a from AppointmentEntity a where a.clinicId = :clinicId "
            + "and a.scheduledStart < :to and a.scheduledEnd > :from "
            + "order by a.scheduledStart asc")
    List<AppointmentEntity> findByClinicIdAndRange(
            @Param("clinicId") UUID clinicId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("select a from AppointmentEntity a where a.doctorStaffId = :doctorStaffId and a.clinicId = :clinicId "
            + "and a.status <> :cancelled and a.status <> :noShow "
            + "and a.scheduledStart < :to and a.scheduledEnd > :from "
            + "order by a.scheduledStart asc")
    List<AppointmentEntity> findActiveByDoctorAndRange(
            @Param("doctorStaffId") UUID doctorStaffId,
            @Param("clinicId") UUID clinicId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("cancelled") AppointmentStatus cancelled,
            @Param("noShow") AppointmentStatus noShow);

    List<AppointmentEntity> findByQuotationIdAndClinicId(UUID quotationId, UUID clinicId);

    List<AppointmentEntity> findByClinicIdAndStatusOrderByUpdatedAtDesc(UUID clinicId, AppointmentStatus status);

    @Query("select distinct a from AppointmentEntity a left join a.quotationItemIds selectedItem where a.quotationId is not null "
            + "and (a.quotationItemId is not null or selectedItem is not null) "
            + "and a.materialsReserved = false and a.status in ('SCHEDULED', 'CONFIRMED')")
    List<AppointmentEntity> findPendingMaterialReservationCandidates();

    Optional<AppointmentEntity> findByClinicIdAndExternalCalendarEventId(UUID clinicId, String externalCalendarEventId);

    @Query("select a from AppointmentEntity a where a.clinicId = :clinicId and a.patientId is null and a.status = 'SCHEDULED' and (a.notes is null or a.notes not like '%[ACKNOWLEDGED]%') order by a.scheduledStart asc")
    List<AppointmentEntity> findByClinicIdAndPatientIdIsNull(@Param("clinicId") UUID clinicId);

    @Query("select count(a) > 0 from AppointmentEntity a where a.roomId = :roomId and a.clinicId = :clinicId "
            + "and a.status not in ('CANCELLED', 'NO_SHOW') "
            + "and a.scheduledStart < :end and a.scheduledEnd > :start "
            + "and (:excludeId is null or a.id != :excludeId)")
    boolean existsOverlappingAppointmentByRoom(
            @Param("roomId") UUID roomId,
            @Param("clinicId") UUID clinicId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") UUID excludeId);
}
