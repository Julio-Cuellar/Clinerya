package com.jclinical.automation.infra.adapters.in.messaging;

import com.jclinical.automation.domain.ports.in.AppointmentRemindersUseCase;
import com.jclinical.automation.infra.config.AutomationRabbitConfig;
import com.jclinical.core.events.AppointmentCancelledEvent;
import com.jclinical.core.events.AppointmentDeletedEvent;
import com.jclinical.core.events.AppointmentRescheduledEvent;
import com.jclinical.core.events.AppointmentScheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Los eventos de la agenda programan, mueven o quitan el recordatorio de cada cita (plan v2, S6). */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderListener {

    private final AppointmentRemindersUseCase reminders;

    @RabbitListener(queues = AutomationRabbitConfig.REMINDER_SCHEDULED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onScheduled(AppointmentScheduledEvent event) {
        log.info(">>>> [AUTOMATIZACION] Programando recordatorio de la cita {}", event.appointmentId());
        reminders.appointmentScheduled(event.clinicId(), event.appointmentId(), event.patientId(), event.doctorStaffId(),
                event.scheduledStart(), event.serviceName());
    }

    @RabbitListener(queues = AutomationRabbitConfig.REMINDER_RESCHEDULED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onRescheduled(AppointmentRescheduledEvent event) {
        log.info(">>>> [AUTOMATIZACION] Moviendo recordatorio de la cita {}", event.appointmentId());
        reminders.appointmentRescheduled(event.clinicId(), event.appointmentId(), event.patientId(), event.doctorStaffId(),
                event.newStart());
    }

    @RabbitListener(queues = AutomationRabbitConfig.REMINDER_CANCELLED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onCancelled(AppointmentCancelledEvent event) {
        reminders.appointmentCancelled(event.appointmentId());
    }

    @RabbitListener(queues = AutomationRabbitConfig.REMINDER_DELETED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onDeleted(AppointmentDeletedEvent event) {
        reminders.appointmentCancelled(event.appointmentId());
    }
}
