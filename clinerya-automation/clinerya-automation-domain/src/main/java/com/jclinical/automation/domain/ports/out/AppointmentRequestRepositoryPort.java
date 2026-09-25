package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.AppointmentRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRequestRepositoryPort {

    AppointmentRequest save(AppointmentRequest request);

    Optional<AppointmentRequest> findByIdAndClinicId(UUID requestId, UUID clinicId);

    /** Solicitudes esperando al medico, las mas antiguas primero. */
    List<AppointmentRequest> findPendingByDoctor(UUID clinicId, UUID doctorStaffId);

    /** Solicitudes abiertas cuyo plazo empezo antes de {@code cutoff} (candidatas a vencer). */
    List<AppointmentRequest> findOverdue(LocalDateTime cutoff);
}
