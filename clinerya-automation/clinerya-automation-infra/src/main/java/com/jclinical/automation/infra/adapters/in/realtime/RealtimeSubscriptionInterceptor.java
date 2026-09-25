package com.jclinical.automation.infra.adapters.in.realtime;

import com.jclinical.automation.domain.service.RealtimeAccessPolicy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.util.UUID;

/** Cada SUBSCRIBE se autoriza con {@link RealtimeAccessPolicy}; lo que no reconoce se niega. */
public class RealtimeSubscriptionInterceptor implements ChannelInterceptor {

    private final RealtimeAccessPolicy policy;

    public RealtimeSubscriptionInterceptor(RealtimeAccessPolicy policy) {
        this.policy = policy;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }
        if (!policy.canSubscribe(userIdOf(accessor.getUser()), accessor.getDestination())) {
            throw new AccessDeniedException("No tienes acceso a este canal.");
        }
        return message;
    }

    private static UUID userIdOf(Principal user) {
        if (user == null) {
            return null;
        }
        try {
            return UUID.fromString(user.getName());
        } catch (IllegalArgumentException notAUserId) {
            return null;
        }
    }
}
