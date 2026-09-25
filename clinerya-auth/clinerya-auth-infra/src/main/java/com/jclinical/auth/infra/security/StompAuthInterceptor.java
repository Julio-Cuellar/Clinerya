package com.jclinical.auth.infra.security;

import com.jclinical.auth.domain.ports.in.ValidateTokenUseCase;
import com.jclinical.auth.infra.adapters.out.JwtTokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.List;

/**
 * Autenticacion de las sesiones STOMP. El navegador no puede mandar cabeceras en el handshake del
 * WebSocket, asi que el JWT viaja en el CONNECT; la sesion queda a nombre del id del usuario. El canal
 * es de solo avisos: el cliente no puede publicar, y para suscribirse necesita sesion (que canal
 * puede escuchar lo decide cada modulo con su propio interceptor).
 */
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    private final JwtTokenProvider tokens;
    private final ValidateTokenUseCase validity;

    public StompAuthInterceptor(JwtTokenProvider tokens, ValidateTokenUseCase validity) {
        this.tokens = tokens;
        this.validity = validity;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT, STOMP -> accessor.setUser(authenticate(accessor.getFirstNativeHeader(AUTHORIZATION)));
            case SEND -> throw new AccessDeniedException("Este canal solo envía avisos; no acepta mensajes.");
            case SUBSCRIBE -> {
                if (accessor.getUser() == null) {
                    throw new AccessDeniedException("Inicia sesión para recibir avisos en tiempo real.");
                }
            }
            default -> {
                // DISCONNECT, UNSUBSCRIBE, ACK...: nada que validar.
            }
        }
        return message;
    }

    private Principal authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)) {
            throw new AccessDeniedException("Falta el token de sesión.");
        }
        String token = authorization.substring(BEARER.length()).trim();
        boolean valid;
        try {
            valid = tokens.validateAccessToken(token) && validity.validate(token);
        } catch (RuntimeException invalid) {
            valid = false;
        }
        if (!valid) {
            throw new AccessDeniedException("La sesión no es válida o ya venció.");
        }
        return new UsernamePasswordAuthenticationToken(tokens.getUserIdFromToken(token), null, List.of());
    }
}
