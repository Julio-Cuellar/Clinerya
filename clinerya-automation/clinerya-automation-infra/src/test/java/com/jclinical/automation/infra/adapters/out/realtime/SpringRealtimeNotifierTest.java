package com.jclinical.automation.infra.adapters.out.realtime;

import com.jclinical.automation.domain.model.RealtimeDestinations;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los avisos se dejan como eventos de la transaccion; un oyente los entrega solo tras el commit. */
class SpringRealtimeNotifierTest {

    private final UUID clinicId = UUID.randomUUID();
    private final List<Object> published = new ArrayList<>();
    private final SpringRealtimeNotifier notifier = new SpringRealtimeNotifier(published::add);

    @Test
    void chatActivityGoesToTheClinicsChatChannelWithoutContent() {
        LocalDateTime at = LocalDateTime.of(2026, 9, 25, 10, 0);

        notifier.chatActivity(clinicId, "5215512345678", at);

        assertEquals(List.of(new RealtimePush(RealtimeDestinations.chats(clinicId),
                new RealtimePush.ChatActivity("5215512345678", at))), published);
    }

    @Test
    void aNewRequestGoesToItsDoctorsInbox() {
        UUID doctorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        notifier.newAppointmentRequest(clinicId, doctorId, requestId);

        assertEquals(List.of(new RealtimePush(RealtimeDestinations.doctorInbox(clinicId, doctorId),
                new RealtimePush.InboxChange(requestId, "NEW"))), published);
    }
}
