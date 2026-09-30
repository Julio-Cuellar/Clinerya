package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.ports.in.AppointmentRemindersUseCase;
import com.jclinical.automation.domain.service.AppointmentReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** Marcar el recordatorio como enviado y encolar su mensaje van juntos: no se repite ni se pierde. */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalAppointmentReminders implements AppointmentRemindersUseCase {

    private final AppointmentReminderService reminders;

    @Override
    @Transactional
    public void appointmentScheduled(UUID clinicId, UUID appointmentId, UUID patientId, UUID doctorStaffId,
                                     LocalDateTime startsAt, String serviceName) {
        reminders.appointmentScheduled(clinicId, appointmentId, patientId, doctorStaffId, startsAt, serviceName);
    }

    @Override
    @Transactional
    public void appointmentRescheduled(UUID clinicId, UUID appointmentId, UUID patientId, UUID doctorStaffId,
                                       LocalDateTime newStart) {
        reminders.appointmentRescheduled(clinicId, appointmentId, patientId, doctorStaffId, newStart);
    }

    @Override
    @Transactional
    public void appointmentCancelled(UUID appointmentId) {
        reminders.appointmentCancelled(appointmentId);
    }

    @Override
    @Transactional
    public int sendDue() {
        return reminders.sendDue();
    }
}
