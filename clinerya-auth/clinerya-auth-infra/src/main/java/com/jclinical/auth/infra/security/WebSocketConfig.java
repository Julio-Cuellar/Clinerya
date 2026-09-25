package com.jclinical.auth.infra.security;

import com.jclinical.auth.domain.ports.in.ValidateTokenUseCase;
import com.jclinical.auth.infra.adapters.out.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * STOMP sobre WebSocket en /ws para avisos en tiempo real. Broker en memoria: con varias instancias
 * habria que pasar a un relay (RabbitMQ). Los latidos cada 10 s evitan que un proxy corte la conexion
 * por inactividad. Va antes que los configuradores de cada modulo para autenticar primero.
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    static final String ENDPOINT = "/ws";
    private static final long HEARTBEAT_MS = 10_000;

    private final JwtTokenProvider tokens;
    private final ValidateTokenUseCase validity;
    private final String[] allowedOrigins;

    public WebSocketConfig(JwtTokenProvider tokens, ValidateTokenUseCase validity,
                           @Value("${app.realtime.allowed-origins:${app.frontend-base-url:http://localhost:5173}}")
                           String allowedOrigins) {
        this.tokens = tokens;
        this.validity = validity;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(ENDPOINT).setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler heartbeats = new ThreadPoolTaskScheduler();
        heartbeats.setPoolSize(1);
        heartbeats.setThreadNamePrefix("ws-heartbeat-");
        heartbeats.initialize();
        registry.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[] {HEARTBEAT_MS, HEARTBEAT_MS})
                .setTaskScheduler(heartbeats);
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompAuthInterceptor(tokens, validity));
    }
}
