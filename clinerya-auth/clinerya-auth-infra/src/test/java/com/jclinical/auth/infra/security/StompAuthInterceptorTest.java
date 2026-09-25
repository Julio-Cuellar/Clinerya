package com.jclinical.auth.infra.security;

import com.jclinical.auth.domain.ports.in.ValidateTokenUseCase;
import com.jclinical.auth.infra.adapters.out.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * El navegador no puede mandar cabeceras en el handshake del WebSocket: el JWT viaja en el CONNECT de
 * STOMP. Sin token valido no hay sesion, y el cliente no puede publicar nada.
 */
class StompAuthInterceptorTest {

    private static final String TOKEN = "token-valido";

    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final ValidateTokenUseCase validity = mock(ValidateTokenUseCase.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private final UUID userId = UUID.randomUUID();

    private StompAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        when(tokens.validateAccessToken(TOKEN)).thenReturn(true);
        when(validity.validate(TOKEN)).thenReturn(true);
        when(tokens.getUserIdFromToken(TOKEN)).thenReturn(userId.toString());
        interceptor = new StompAuthInterceptor(tokens, validity);
    }

    @Test
    void aValidTokenOnConnectOpensASessionAsThatUser() {
        Message<?> result = interceptor.preSend(frame(StompCommand.CONNECT, "Bearer " + TOKEN, null), channel);

        Principal user = StompHeaderAccessor.wrap(result).getUser();
        assertEquals(userId.toString(), user.getName());
    }

    @Test
    void connectWithoutTokenIsRejected() {
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(frame(StompCommand.CONNECT, null, null), channel));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(frame(StompCommand.CONNECT, TOKEN, null), channel));
    }

    @Test
    void anExpiredOrRevokedTokenIsRejected() {
        when(tokens.validateAccessToken("vencido")).thenReturn(false);
        when(validity.validate("revocado")).thenReturn(false);
        when(tokens.validateAccessToken("revocado")).thenReturn(true);

        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer vencido", null), channel));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer revocado", null), channel));
    }

    @Test
    void clientsCannotPublishMessages() {
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(frame(StompCommand.SEND, null, session()), channel));
    }

    @Test
    void subscribingRequiresASession() {
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, null), channel));

        Message<?> allowed = frame(StompCommand.SUBSCRIBE, null, session());
        assertSame(allowed, interceptor.preSend(allowed, channel), "la autorizacion del canal la decide cada modulo");
    }

    @Test
    void otherFramesPassThrough() {
        Message<?> disconnect = frame(StompCommand.DISCONNECT, null, null);
        assertSame(disconnect, interceptor.preSend(disconnect, channel));
    }

    private Principal session() {
        return new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.List.of());
    }

    private static Message<byte[]> frame(StompCommand command, String authorization, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
            accessor.setDestination("/topic/x");
        }
        accessor.setUser(user);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
