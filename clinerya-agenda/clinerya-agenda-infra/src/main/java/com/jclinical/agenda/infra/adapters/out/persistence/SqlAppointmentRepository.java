package com.jclinical.agenda.infra.adapters.out.persistence;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlAppointmentRepository implements AppointmentRepositoryPort {

    private final SpringDataAppointmentRepository springRepository;
    private final AppointmentMapper mapper;

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentEntity entity = mapper.toEntity(appointment);
        AppointmentEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Appointment> findByIdAndClinicId(UUID appointmentId, UUID clinicId) {
        return springRepository.findByIdAndClinicId(appointmentId, clinicId).map(mapper::toDomain);
    }

    @Override
    public List<Appointment> findByClinicIdAndRange(UUID clinicId, LocalDateTime from, LocalDateTime to) {
        return springRepository.findByClinicIdAndRange(clinicId, from, to).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Appointment> findActiveByDoctorAndRange(UUID doctorStaffId, UUID clinicId, LocalDateTime from, LocalDateTime to) {
        return springRepository.findActiveByDoctorAndRange(
                        doctorStaffId, clinicId, from, to, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findByQuotationIdAndClinicId(UUID quotationId, UUID clinicId) {
        return springRepository.findByQuotationIdAndClinicId(quotationId, clinicId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Appointment> findCompletedByClinicId(UUID clinicId) {
        return springRepository.findByClinicIdAndStatusOrderByUpdatedAtDesc(clinicId, AppointmentStatus.COMPLETED)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findPendingMaterialReservationCandidates() {
        return springRepository.findPendingMaterialReservationCandidates().stream().map(mapper::toDomain).toList();
     }

    @Override
    public Optional<Appointment> findByClinicIdAndExternalCalendarEventId(UUID clinicId, String externalCalendarEventId) {
        return springRepository.findByClinicIdAndExternalCalendarEventId(clinicId, externalCalendarEventId).map(mapper::toDomain);
    }

    @Override
    public List<Appointment> findByClinicIdAndPatientIdIsNull(UUID clinicId) {
        return springRepository.findByClinicIdAndPatientIdIsNull(clinicId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsOverlappingAppointmentByRoom(UUID roomId, UUID clinicId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId) {
        if (roomId == null) {
            return false;
        }
        return springRepository.existsOverlappingAppointmentByRoom(roomId, clinicId, start, end, excludeAppointmentId);
    }

    @Override
    public void delete(Appointment appointment) {
        springRepository.deleteById(appointment.getId());
    }
}
