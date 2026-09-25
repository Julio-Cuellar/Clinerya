package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AppointmentRequest;
import com.jclinical.automation.domain.model.DoctorChannel;
import com.jclinical.automation.domain.model.DoctorNotice;
import com.jclinical.automation.domain.ports.in.RemindPendingRequestsUseCase;
import com.jclinical.automation.domain.ports.out.DoctorAlertPort;
import com.jclinical.automation.domain.ports.out.DoctorChannelRepositoryPort;
import com.jclinical.automation.domain.ports.out.DoctorNoticeQueuePort;
import com.jclinical.automation.domain.ports.out.DoctorReplyPort;
import com.jclinical.automation.domain.ports.out.PendingRequestReminderPort;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Avisos al medico por WhatsApp (D8): solicitud nueva, recordatorio si a las 4 h sigue sin respuesta
 * y, si escribe, que responde en Clinerya. Los avisos solo llevan la fecha y el enlace a la bandeja;
 * nada del paciente sale por este canal.
 */
public class DoctorNotificationService implements DoctorAlertPort, DoctorReplyPort, RemindPendingRequestsUseCase {

    static final Duration REMINDER_AFTER = Duration.ofHours(4);
    static final String ANSWER_IN_CLINERYA =
            "Este número solo envía avisos. Responde las solicitudes de cita desde Clinerya";

    private final DoctorChannelRepositoryPort channels;
    private final DoctorNoticeQueuePort notices;
    private final PendingRequestReminderPort reminders;
    private final String inboxUrl;
    private final Clock clock;

    public DoctorNotificationService(DoctorChannelRepositoryPort channels, DoctorNoticeQueuePort notices,
                                     PendingRequestReminderPort reminders, String inboxUrl, Clock clock) {
        this.channels = channels;
        this.notices = notices;
        this.reminders = reminders;
        this.inboxUrl = inboxUrl;
        this.clock = clock;
    }

    @Override
    public void newRequest(AppointmentRequest request) {
        activeChannelOf(request).ifPresent(channel -> notices.enqueue(
                notice(channel, "Tienes una nueva solicitud de cita para el " + SlotLabel.of(request.start()))));
    }

    @Override
    public int remindPending() {
        LocalDateTime now = LocalDateTime.now(clock);
        int reminded = 0;
        for (AppointmentRequest request : reminders.findPendingNotRemindedBefore(now.minus(REMINDER_AFTER))) {
            Optional<DoctorChannel> channel = request.isOverdueAt(now) ? Optional.empty() : activeChannelOf(request);
            if (channel.isPresent()) {
                notices.enqueue(notice(channel.get(),
                        "Recordatorio: sigue pendiente la solicitud de cita del " + SlotLabel.of(request.start())));
                reminded++;
            }
            reminders.markReminded(request.id(), now);
        }
        return reminded;
    }

    @Override
    public boolean replyIfDoctor(UUID clinicId, String fromPhone) {
        Optional<DoctorChannel> doctor = channels.findActiveByPhone(clinicId, fromPhone);
        doctor.ifPresent(channel -> notices.enqueue(notice(channel, ANSWER_IN_CLINERYA)));
        return doctor.isPresent();
    }

    private Optional<DoctorChannel> activeChannelOf(AppointmentRequest request) {
        return channels.find(request.clinicId(), request.doctorStaffId()).filter(DoctorChannel::active);
    }

    /** Texto libre dentro de la ventana de 24 h; fuera, la plantilla recibe el resumen y el enlace. */
    private DoctorNotice notice(DoctorChannel channel, String summary) {
        return new DoctorNotice(channel.clinicId(), channel.phone(), summary + ": " + inboxUrl, List.of(summary, inboxUrl));
    }
}
