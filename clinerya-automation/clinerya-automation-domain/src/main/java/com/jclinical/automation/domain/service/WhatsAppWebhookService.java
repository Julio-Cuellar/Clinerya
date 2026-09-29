package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.DeliveryStatusUpdate;
import com.jclinical.automation.domain.model.WebhookPayload;
import com.jclinical.automation.domain.model.WebhookSignature;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage.Kind;
import com.jclinical.automation.domain.model.WhatsAppMessageReceivedEvent;
import com.jclinical.automation.domain.ports.in.ReceiveWhatsAppWebhookUseCase;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.automation.domain.ports.out.ChatContactsPort;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.ports.out.DeliveryStatusPort;
import com.jclinical.automation.domain.ports.out.DoctorChannelRepositoryPort;
import com.jclinical.automation.domain.ports.out.InboundMessageLedgerPort;
import com.jclinical.automation.domain.ports.out.WebhookPayloadParserPort;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Webhook de WhatsApp de cada clinica. La llave de la URL identifica a la clinica; la firma con el
 * secreto de su app prueba que el cuerpo viene de Meta. Solo registra y publica: la conversacion
 * corre despues, fuera de la peticion, para responderle a Meta de inmediato.
 */
public class WhatsAppWebhookService implements ReceiveWhatsAppWebhookUseCase {

    static final String SUBSCRIBE_MODE = "subscribe";
    /** Como se ve en el historial un mensaje que no es texto (audio, imagen...). */
    static final String UNSUPPORTED_PLACEHOLDER = "[Mensaje que no es texto]";

    private final ChannelSettingsRepositoryPort settings;
    private final WebhookPayloadParserPort parser;
    private final InboundMessageLedgerPort ledger;
    private final DeliveryStatusPort deliveryStatus;
    private final ChatHistoryPort history;
    private final DoctorChannelRepositoryPort doctorChannels;
    private final DomainEventPublisherPort events;
    private final Clock clock;
    private ChatContactsPort chatContacts;

    public WhatsAppWebhookService(ChannelSettingsRepositoryPort settings, WebhookPayloadParserPort parser,
                                  InboundMessageLedgerPort ledger, DeliveryStatusPort deliveryStatus,
                                  ChatHistoryPort history, DoctorChannelRepositoryPort doctorChannels,
                                  DomainEventPublisherPort events, Clock clock) {
        this.settings = settings;
        this.parser = parser;
        this.ledger = ledger;
        this.deliveryStatus = deliveryStatus;
        this.history = history;
        this.doctorChannels = doctorChannels;
        this.events = events;
        this.clock = clock;
    }

    /** Donde se guarda el nombre de perfil de WhatsApp de cada contacto (ficha del contacto en Chats). */
    public void setChatContacts(ChatContactsPort chatContacts) {
        this.chatContacts = chatContacts;
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
        LocalDateTime now = LocalDateTime.now(clock);
        WebhookPayload payload = parser.parse(rawBody);
        if (clinic.enabled()) {
            for (DeliveryStatusUpdate status : payload.statuses()) {
                if (isForThisNumber(clinic, status.phoneNumberId())) {
                    deliveryStatus.record(clinic.clinicId(), status);
                }
            }
        }
        for (WhatsAppInboundMessage message : payload.messages()) {
            boolean forThisNumber = isForThisNumber(clinic, message.phoneNumberId());
            if (forThisNumber && ledger.recordIfNew(clinic.clinicId(), message.waMessageId(), message.fromPhone(), now)) {
                keepInHistory(clinic.clinicId(), message, now);
                if (clinic.enabled()) {
                    events.publish(DomainEventRoutingKeys.WHATSAPP_MESSAGE_RECEIVED, new WhatsAppMessageReceivedEvent(
                            UUID.randomUUID(), clinic.clinicId(), message.waMessageId(), message.fromPhone(),
                            message.kind(), message.text(), message.selectedOptionId(), now));
                }
            }
        }
        return clinic.enabled() ? Receipt.ACCEPTED : Receipt.IGNORED;
    }

    /**
     * El mensaje del paciente se guarda al recibirlo, en la misma transaccion que lo registra como
     * recibido y antes de que corra el asistente: si el asistente esta apagado, Gemini falla o el
     * procesamiento se revierte, el personal igual lo ve y puede responder. Los mensajes de un medico
     * no entran al historial de pacientes (se le contesta que responda en Clinerya).
     */
    private void keepInHistory(UUID clinicId, WhatsAppInboundMessage message, LocalDateTime now) {
        if (doctorChannels.findActiveByPhone(clinicId, message.fromPhone()).isPresent()) {
            return;
        }
        String text = message.kind() == Kind.UNSUPPORTED ? UNSUPPORTED_PLACEHOLDER : message.text();
        history.record(new ChatMessage(UUID.randomUUID(), clinicId, message.fromPhone(), ChatMessage.Direction.INBOUND,
                text, List.of(), now));
        if (chatContacts != null && message.profileName() != null && !message.profileName().isBlank()) {
            chatContacts.saveProfileName(clinicId, message.fromPhone(), message.profileName().strip(), now);
        }
    }

    private static boolean isForThisNumber(ChannelSettings clinic, String phoneNumberId) {
        return clinic.whatsappPhoneNumberId() != null && clinic.whatsappPhoneNumberId().equals(phoneNumberId);
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
