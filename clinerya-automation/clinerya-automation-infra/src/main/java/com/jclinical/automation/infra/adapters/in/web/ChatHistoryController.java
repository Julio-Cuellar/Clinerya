package com.jclinical.automation.infra.adapters.in.web;

import com.jclinical.automation.domain.model.ChatAccess;
import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.ports.in.ReadChatHistoryUseCase;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Chats de WhatsApp de la clinica. La lista no trae contenido; leer un hilo queda auditado; la
 * auditoria de lecturas es solo para el administrador.
 */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/automation")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ReadChatHistoryUseCase chats;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/chats")
    public List<ChatSummary> listChats(@PathVariable UUID clinicId) {
        return chats.listChats(currentUserResolver.getCurrentUserId(), clinicId);
    }

    @GetMapping("/chats/{phone}/messages")
    public List<ChatMessage> readMessages(@PathVariable UUID clinicId, @PathVariable String phone,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
                                          @RequestParam(required = false) Integer limit,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after) {
        if (after != null) {
            // El chat abierto en pantalla pide lo que llego despues del ultimo mensaje que muestra.
            return chats.readNewMessages(currentUserResolver.getCurrentUserId(), clinicId, phone, after);
        }
        return chats.readMessages(currentUserResolver.getCurrentUserId(), clinicId, phone, before, limit);
    }

    @GetMapping("/chat-access-log")
    public List<ChatAccess> accessLog(@PathVariable UUID clinicId,
                                      @RequestParam(required = false) String phone,
                                      @RequestParam(required = false) UUID userId,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return chats.accessLog(currentUserResolver.getCurrentUserId(), clinicId, phone, userId, from, to);
    }
}
