package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.WebhookSignature;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage;
import com.jclinical.automation.domain.model.WhatsAppMessageReceivedEvent;
import com.jclinical.automation.domain.ports.in.ReceiveWhatsAppWebhookUseCase;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.automation.domain.ports.out.DeliveryStatusPort;
import com.jclinical.automation.domain.ports.out.InboundMessageLedgerPort;
import com.jclinical.automation.domain.ports.out.WebhookPayloadParserPort;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Webhook de WhatsApp de cada clinica. La llave de la URL identifica a la clinica; la firma con el
 * secreto de su app prueba que el cuerpo viene de Meta. Solo registra y publica: la conversacion
 * corre despues, fuera de la peticion, para responderle a Meta de inmediato.
 */
public class WhatsAppWebhookService implements ReceiveWhatsAppWebhookUseCase {

    static final String SUBSCRIBE_MODE = "subscribe";

    private final ChannelSettingsRepositoryPort settings;
    private final WebhookPayloadParserPort parser;
    private final InboundMessageLedgerPort ledger;
    private final DeliveryStatusPort deliveryStatus;
    private final DomainEventPublisherPort events;
    private final Clock clock;

    public WhatsAppWebhookService(ChannelSettingsRepositoryPort settings, WebhookPayloadParserPort parser,
                                  InboundMessageLedgerPort ledger, DeliveryStatusPort deliveryStatus,
                                  DomainEventPublisherPort events, Clock clock) {
        this.settings = settings;
        this.parser = parser;
        this.ledger = ledger;
        this.deliveryStatus = deliveryStatus;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Optional<String> verifySubscription(String webhookKey, String mode, String verifyToken, String challenge) {
        if (!SUBSCRIBE_MODE.equals(mode) || verifyToken == null || challenge == null) {
            return Optional.empty();
        }
        return clinicOf(webhookKey)
                .filter(clinic -> clinic.verifyToken() != null && sameSecret(clinic.verifyToken(), verifyToken))
                .map(clinic -> challenge);
    }

    @Override
    public Receipt receive(String webhookKey, byte[] rawBody, String signatureHeader) {
        Optional<ChannelSettings> found = clinicOf(webhookKey);
        if (found.isEmpty()) {
            return Receipt.UNKNOWN_WEBHOOK;
        }
        ChannelSettings clinic = found.get();
        if (!WebhookSignature.isValid(rawBody, signatureHeader, clinic.whatsappAppSecret())) {
            return Receipt.INVALID_SIGNATURE;
        }
        if (!clinic.enabled()) {
            return Receipt.IGNORED;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (WhatsAppInboundMessage message : parser.parse(rawBody).messages()) {
            boolean forThisNumber = clinic.whatsappPhoneNumberId() != null
                    && clinic.whatsappPhoneNumberId().equals(message.phoneNumberId());
            if (forThisNumber && ledger.recordIfNew(clinic.clinicId(), message.waMessageId(), message.fromPhone(), now)) {
                events.publish(DomainEventRoutingKeys.WHATSAPP_MESSAGE_RECEIVED, new WhatsAppMessageReceivedEvent(
                        UUID.randomUUID(), clinic.clinicId(), message.waMessageId(), message.fromPhone(),
                        message.kind(), message.text(), message.selectedOptionId(), now));
            }
        }
        return Receipt.ACCEPTED;
    }

    private Optional<ChannelSettings> clinicOf(String webhookKey) {
        if (webhookKey == null || webhookKey.isBlank()) {
            return Optional.empty();
        }
        return settings.findByWebhookKey(webhookKey);
    }

    private static boolean sameSecret(String expected, String received) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8));
    }
}
