package com.jclinical.automation.infra.adapters.in.realtime;

import com.jclinical.automation.domain.model.RealtimeDestinations;
import com.jclinical.automation.domain.service.RealtimeAccessPolicy;
import com.jclinical.core.security.StaffPermission;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Cada SUBSCRIBE pasa por la politica de la automatizacion; lo que no autoriza se corta ahi. */
class RealtimeSubscriptionInterceptorTest {

    private final UUID clinicId = UUID.randomUUID();
    private final UUID allowedUser = UUID.randomUUID();
    private final UUID otherUser = UUID.randomUUID();

    private final RealtimeSubscriptionInterceptor interceptor = new RealtimeSubscriptionInterceptor(new RealtimeAccessPolicy(
            (clinic, user, permission) -> permission == StaffPermission.VIEW_PATIENTS && user.equals(allowedUser),
            (clinic, user) -> Optional.empty()));

    @Test
    void anAuthorizedSubscriptionGoesThrough() {
        Message<byte[]> subscribe = frame(StompCommand.SUBSCRIBE, allowedUser.toString(), RealtimeDestinations.chats(clinicId));

        assertSame(subscribe, interceptor.preSend(subscribe, null));
    }

    @Test
    void anUnauthorizedSubscriptionIsRejected() {
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, otherUser.toString(), RealtimeDestinations.chats(clinicId)), null));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, allowedUser.toString(), "/topic/otra-cosa"), null));
    }

    @Test
    void aSessionWithoutAValidUserIsRejected() {
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "no-es-uuid", RealtimeDestinations.chats(clinicId)), null));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, null, RealtimeDestinations.chats(clinicId)), null));
    }

    @Test
    void otherFramesAreNotItsBusiness() {
        Message<byte[]> connect = frame(StompCommand.CONNECT, null, null);
        assertSame(connect, interceptor.preSend(connect, null));
    }

    private static Message<byte[]> frame(StompCommand command, String user, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (user != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
