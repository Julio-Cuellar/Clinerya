package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.model.ChatContact;
import com.jclinical.automation.domain.ports.in.ViewChatContactUseCase;
import com.jclinical.automation.domain.service.ChatContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** La ficha lee chat, pacientes y agenda en una sola transaccion de lectura. */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalChatContactUseCase implements ViewChatContactUseCase {

    private final ChatContactService contacts;

    @Override
    @Transactional(readOnly = true)
    public ChatContact contact(UUID actingUserId, UUID clinicId, String phone) {
        return contacts.contact(actingUserId, clinicId, phone);
    }
}
