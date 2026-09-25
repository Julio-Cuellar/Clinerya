package com.jclinical.automation.infra.adapters.out.realtime;

import com.jclinical.automation.domain.model.RealtimeDestinations;
import com.jclinical.automation.domain.ports.out.RealtimeNotifierPort;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Deja cada aviso como evento de la transaccion en curso; {@link RealtimePushListener} lo entrega solo
 * si la transaccion se confirma, para no anunciar algo que se revirtio.
 */
public class SpringRealtimeNotifier implements RealtimeNotifierPort {

    static final String NEW_REQUEST = "NEW";

    private final ApplicationEventPublisher events;

    public SpringRealtimeNotifier(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override
    public void chatActivity(UUID clinicId, String phone, LocalDateTime at) {
        events.publishEvent(new RealtimePush(RealtimeDestinations.chats(clinicId), new RealtimePush.ChatActivity(phone, at)));
    }

    @Override
    public void newAppointmentRequest(UUID clinicId, UUID doctorStaffId, UUID requestId) {
        events.publishEvent(new RealtimePush(RealtimeDestinations.doctorInbox(clinicId, doctorStaffId),
                new RealtimePush.InboxChange(requestId, NEW_REQUEST)));
    }
}
