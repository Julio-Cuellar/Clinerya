package com.jclinical.agenda.domain.ports.out;

import com.jclinical.agenda.domain.model.Appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepositoryPort {

    Appointment save(Appointment appointment);

    Optional<Appointment> findByIdAndClinicId(UUID appointmentId, UUID clinicId);

    List<Appointment> findByClinicIdAndRange(UUID clinicId, LocalDateTime from, LocalDateTime to);

    List<Appointment> findActiveByDoctorAndRange(UUID doctorStaffId, UUID clinicId, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByQuotationIdAndClinicId(UUID quotationId, UUID clinicId);

    List<Appointment> findByPatientIdAndClinicId(UUID patientId, UUID clinicId);

    List<Appointment> findCompletedByClinicId(UUID clinicId);

    List<Appointment> findPendingMaterialReservationCandidates();

    Optional<Appointment> findByClinicIdAndExternalCalendarEventId(UUID clinicId, String externalCalendarEventId);

    List<Appointment> findByClinicIdAndPatientIdIsNull(UUID clinicId);

    boolean existsOverlappingAppointmentByRoom(UUID roomId, UUID clinicId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId);

    void delete(Appointment appointment);

}
