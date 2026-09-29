package com.jclinical.automation.infra.adapters.in.web;

import com.jclinical.automation.domain.model.ChatAttention;
import com.jclinical.automation.domain.model.ChatContact;
import com.jclinical.automation.domain.ports.in.ManageChatAttentionUseCase;
import com.jclinical.automation.domain.ports.in.ViewChatContactUseCase;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Atencion humana en Chats: ver quien atiende un chat, tomarlo o regresarlo al agente, y escribirle
 * al paciente mientras lo atiende una persona. Tambien la ficha breve de quien escribe.
 */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/automation/chats/{phone}")
@RequiredArgsConstructor
public class ChatAttentionController {

    private final ManageChatAttentionUseCase attention;
    private final ViewChatContactUseCase contacts;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/contact")
    public ChatContact contact(@PathVariable UUID clinicId, @PathVariable String phone) {
        return contacts.contact(currentUserResolver.getCurrentUserId(), clinicId, phone);
    }

    @GetMapping("/attention")
    public ChatAttention attention(@PathVariable UUID clinicId, @PathVariable String phone) {
        return attention.attention(currentUserResolver.getCurrentUserId(), clinicId, phone);
    }

    @PutMapping("/attention")
    public ChatAttention setAttention(@PathVariable UUID clinicId, @PathVariable String phone,
                                      @RequestBody AttentionRequest request) {
        UUID userId = currentUserResolver.getCurrentUserId();
        return request.human()
                ? attention.takeOver(userId, clinicId, phone)
                : attention.release(userId, clinicId, phone);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendMessage(@PathVariable UUID clinicId, @PathVariable String phone, @RequestBody StaffMessageRequest request) {
        attention.sendMessage(currentUserResolver.getCurrentUserId(), clinicId, phone, request.text());
    }

    public record AttentionRequest(boolean human) {}

    public record StaffMessageRequest(String text) {}
}
