package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.service.RealtimeAccessPolicy;
import com.jclinical.automation.infra.adapters.in.realtime.RealtimeSubscriptionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/** Suma la autorizacion de suscripciones de la automatizacion al canal STOMP (la sesion la abre auth). */
@Configuration
public class AutomationRealtimeConfig implements WebSocketMessageBrokerConfigurer {

    private final RealtimeAccessPolicy policy;

    public AutomationRealtimeConfig(RealtimeAccessPolicy policy) {
        this.policy = policy;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new RealtimeSubscriptionInterceptor(policy));
    }
}
