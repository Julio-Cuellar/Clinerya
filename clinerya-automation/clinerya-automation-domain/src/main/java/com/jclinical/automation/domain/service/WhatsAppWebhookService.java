package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.ports.in.ReceiveWhatsAppWebhookUseCase;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.automation.domain.ports.out.InboundMessageLedgerPort;
import com.jclinical.automation.domain.ports.out.WebhookPayloadParserPort;
import com.jclinical.core.events.DomainEventPublisherPort;

import java.time.Clock;
import java.util.Optional;

public class WhatsAppWebhookService implements ReceiveWhatsAppWebhookUseCase {

    public WhatsAppWebhookService(ChannelSettingsRepositoryPort settings, WebhookPayloadParserPort parser,
                                  InboundMessageLedgerPort ledger, DomainEventPublisherPort events, Clock clock) {
    }

    @Override
    public Optional<String> verifySubscription(String webhookKey, String mode, String verifyToken, String challenge) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public Receipt receive(String webhookKey, byte[] rawBody, String signatureHeader) {
        throw new UnsupportedOperationException("pendiente");
    }
}
