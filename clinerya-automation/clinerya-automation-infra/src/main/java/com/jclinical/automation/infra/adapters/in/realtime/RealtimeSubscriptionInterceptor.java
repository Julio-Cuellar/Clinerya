package com.jclinical.automation.infra.adapters.in.realtime;

import com.jclinical.automation.domain.service.RealtimeAccessPolicy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

public class RealtimeSubscriptionInterceptor implements ChannelInterceptor {

    public RealtimeSubscriptionInterceptor(RealtimeAccessPolicy policy) {
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        throw new UnsupportedOperationException("pendiente");
    }
}
