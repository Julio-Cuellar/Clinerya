package com.jclinical.integrations.infra.adapters.in.messaging;

import com.jclinical.core.events.AppointmentCancelledEvent;
import com.jclinical.core.events.AppointmentDeletedEvent;
import com.jclinical.core.events.AppointmentRescheduledEvent;
import com.jclinical.core.events.AppointmentScheduledEvent;
import com.jclinical.integrations.infra.config.IntegrationsRabbitConfig;
import com.jclinical.integrations.infra.service.CalendarSyncOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarSyncListener {

    private final CalendarSyncOrchestrator syncOrchestrator;

    @RabbitListener(queues = IntegrationsRabbitConfig.APPOINTMENT_SCHEDULED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onAppointmentScheduled(AppointmentScheduledEvent event) {
        log.info(">>>> [INTEGRACIONES] Sincronizando cita {} agendada con Google Calendar", event.appointmentId());
        syncOrchestrator.onAppointmentScheduled(
                event.clinicId(), event.appointmentId(), event.doctorStaffId(), event.scheduledStart(), event.scheduledEnd());
    }

    @RabbitListener(queues = IntegrationsRabbitConfig.APPOINTMENT_RESCHEDULED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onAppointmentRescheduled(AppointmentRescheduledEvent event) {
        log.info(">>>> [INTEGRACIONES] Sincronizando cita {} reagendada con Google Calendar", event.appointmentId());
        syncOrchestrator.onAppointmentRescheduled(event.clinicId(), event.appointmentId(), event.newStart(), event.newEnd());
    }

    @RabbitListener(queues = IntegrationsRabbitConfig.APPOINTMENT_CANCELLED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info(">>>> [INTEGRACIONES] Sincronizando cancelación de cita {} con Google Calendar", event.appointmentId());
        syncOrchestrator.onAppointmentCancelled(event.clinicId(), event.appointmentId());
    }

    @RabbitListener(queues = IntegrationsRabbitConfig.APPOINTMENT_DELETED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onAppointmentDeleted(AppointmentDeletedEvent event) {
        log.info(">>>> [INTEGRACIONES] Sincronizando borrado de cita {} con Google Calendar", event.appointmentId());
        syncOrchestrator.onAppointmentDeleted(event.clinicId(), event.doctorStaffId(), event.externalCalendarEventId());
    }
}
