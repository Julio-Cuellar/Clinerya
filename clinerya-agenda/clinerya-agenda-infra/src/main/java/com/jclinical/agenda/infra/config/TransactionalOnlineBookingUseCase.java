package com.jclinical.agenda.infra.config;

import com.jclinical.agenda.domain.model.BookableSlot;
import com.jclinical.agenda.domain.model.SlotHold;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.agenda.domain.service.OnlineBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * El bloqueo por medico (pg_advisory_xact_lock) vive lo que dura la transaccion: apartar debe
 * revalidar y guardar dentro de la misma, o el bloqueo no protege nada.
 */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalOnlineBookingUseCase implements OnlineBookingUseCase {

    private final OnlineBookingService onlineBookingService;

    @Override
    @Transactional(readOnly = true)
    public List<BookableSlot> findAvailableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit) {
        return onlineBookingService.findAvailableSlots(clinicId, doctorStaffId, from, days, limit);
    }

    @Override
    @Transactional
    public SlotHold holdSlot(HoldSlotCommand command) {
        return onlineBookingService.holdSlot(command);
    }

    @Override
    @Transactional
    public void releaseHold(UUID clinicId, UUID holdId) {
        onlineBookingService.releaseHold(clinicId, holdId);
    }

    @Override
    @Transactional
    public UUID bookHeldSlot(BookHeldSlotCommand command) {
        return onlineBookingService.bookHeldSlot(command);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> doctorStaffIdOfUser(UUID clinicId, UUID userId) {
        return onlineBookingService.doctorStaffIdOfUser(clinicId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public VisitSummary visitSummary(UUID clinicId, UUID patientId) {
        return onlineBookingService.visitSummary(clinicId, patientId);
    }

    @Override
    @Transactional
    public void cancelByPatient(UUID clinicId, UUID appointmentId, UUID patientId, String reason) {
        onlineBookingService.cancelByPatient(clinicId, appointmentId, patientId, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingAppointment> upcomingAppointments(UUID clinicId, UUID patientId, int limit) {
        return onlineBookingService.upcomingAppointments(clinicId, patientId, limit);
    }
}
