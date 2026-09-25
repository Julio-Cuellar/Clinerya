package com.jclinical.auth.infra.security;

import com.jclinical.auth.domain.ports.in.ValidateTokenUseCase;
import com.jclinical.auth.infra.adapters.out.JwtTokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

public class StompAuthInterceptor implements ChannelInterceptor {

    public StompAuthInterceptor(JwtTokenProvider tokens, ValidateTokenUseCase validity) {
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        throw new UnsupportedOperationException("pendiente");
    }
}
