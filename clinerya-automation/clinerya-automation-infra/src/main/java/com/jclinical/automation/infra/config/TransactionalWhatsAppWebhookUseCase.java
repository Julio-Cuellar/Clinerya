package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.ports.in.ReceiveWhatsAppWebhookUseCase;
import com.jclinical.automation.domain.service.WhatsAppWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** El registro del id y el evento en el outbox se guardan juntos: sin mensaje perdido ni duplicado. */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalWhatsAppWebhookUseCase implements ReceiveWhatsAppWebhookUseCase {

    private final WhatsAppWebhookService webhook;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> verifySubscription(String webhookKey, String mode, String verifyToken, String challenge) {
        return webhook.verifySubscription(webhookKey, mode, verifyToken, challenge);
    }

    @Override
    @Transactional
    public Receipt receive(String webhookKey, byte[] rawBody, String signatureHeader) {
        return webhook.receive(webhookKey, rawBody, signatureHeader);
    }
}
