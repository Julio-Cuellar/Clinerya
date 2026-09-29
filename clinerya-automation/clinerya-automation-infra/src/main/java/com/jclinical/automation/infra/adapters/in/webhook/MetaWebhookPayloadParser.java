package com.jclinical.automation.infra.adapters.in.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.DeliveryStatusUpdate;
import com.jclinical.automation.domain.model.WebhookPayload;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage.Kind;
import com.jclinical.automation.domain.ports.out.WebhookPayloadParserPort;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;

/**
 * Lee el JSON del webhook de WhatsApp Cloud API (entry[].changes[].value.messages[]). Los avisos de
 * estado no son mensajes; un cuerpo ilegible se trata como vacio (ya paso la validacion de firma).
 */
public class MetaWebhookPayloadParser implements WebhookPayloadParserPort {

    private final ObjectMapper objectMapper;
    private final ZoneId zone;

    public MetaWebhookPayloadParser(ObjectMapper objectMapper, ZoneId zone) {
        this.objectMapper = objectMapper;
        this.zone = zone;
    }

    @Override
    public WebhookPayload parse(byte[] rawBody) {
        if (rawBody == null || rawBody.length == 0) {
            return WebhookPayload.EMPTY;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (IOException unreadable) {
            return WebhookPayload.EMPTY;
        }
        if (root == null) {
            return WebhookPayload.EMPTY;
        }
        List<WhatsAppInboundMessage> messages = new ArrayList<>();
        List<DeliveryStatusUpdate> statuses = new ArrayList<>();
        for (JsonNode entry : root.path("entry")) {
            for (JsonNode change : entry.path("changes")) {
                JsonNode value = change.path("value");
                String phoneNumberId = value.path("metadata").path("phone_number_id").asText(null);
                Map<String, String> profileNames = profileNames(value.path("contacts"));
                for (JsonNode message : value.path("messages")) {
                    WhatsAppInboundMessage parsed = toMessage(phoneNumberId, message);
                    messages.add(withProfileName(parsed, profileNames.get(parsed.fromPhone())));
                }
                for (JsonNode status : value.path("statuses")) {
                    toStatus(phoneNumberId, status).ifPresent(statuses::add);
                }
            }
        }
        return new WebhookPayload(messages, statuses);
    }

    /** Meta manda el nombre de perfil de cada remitente en contacts[] (wa_id -> profile.name). */
    private static Map<String, String> profileNames(JsonNode contacts) {
        Map<String, String> names = new HashMap<>();
        for (JsonNode contact : contacts) {
            String waId = contact.path("wa_id").asText(null);
            String name = contact.path("profile").path("name").asText(null);
            if (waId != null && name != null && !name.isBlank()) {
                names.put(waId, name);
            }
        }
        return names;
    }

    private static WhatsAppInboundMessage withProfileName(WhatsAppInboundMessage message, String profileName) {
        return profileName == null ? message : new WhatsAppInboundMessage(message.phoneNumberId(), message.waMessageId(),
                message.fromPhone(), message.kind(), message.text(), message.selectedOptionId(), message.sentAt(), profileName);
    }

    /** sent, delivered, read y failed; cualquier otro estado de Meta se ignora. */
    private Optional<DeliveryStatusUpdate> toStatus(String phoneNumberId, JsonNode status) {
        DeliveryStatusUpdate.Status value;
        try {
            value = DeliveryStatusUpdate.Status.valueOf(status.path("status").asText("").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
        JsonNode code = status.at("/errors/0/code");
        return Optional.of(new DeliveryStatusUpdate(phoneNumberId, status.path("id").asText(null), value,
                sentAt(status.path("timestamp").asText("")), code.isMissingNode() ? null : code.asText()));
    }

    private WhatsAppInboundMessage toMessage(String phoneNumberId, JsonNode message) {
        String id = message.path("id").asText(null);
        String from = message.path("from").asText(null);
        LocalDateTime sentAt = sentAt(message.path("timestamp").asText(""));
        return switch (message.path("type").asText("")) {
            case "text" -> new WhatsAppInboundMessage(phoneNumberId, id, from, Kind.TEXT,
                    message.path("text").path("body").asText(""), null, sentAt);
            case "interactive" -> interactive(phoneNumberId, id, from, message.path("interactive"), sentAt);
            case "button" -> new WhatsAppInboundMessage(phoneNumberId, id, from, Kind.OPTION,
                    message.path("button").path("text").asText(null), message.path("button").path("payload").asText(null), sentAt);
            default -> new WhatsAppInboundMessage(phoneNumberId, id, from, Kind.UNSUPPORTED, null, null, sentAt);
        };
    }

    private static WhatsAppInboundMessage interactive(String phoneNumberId, String id, String from, JsonNode interactive,
                                                      LocalDateTime sentAt) {
        JsonNode reply = interactive.has("button_reply") ? interactive.path("button_reply") : interactive.path("list_reply");
        if (reply.isMissingNode() || !reply.has("id")) {
            return new WhatsAppInboundMessage(phoneNumberId, id, from, Kind.UNSUPPORTED, null, null, sentAt);
        }
        return new WhatsAppInboundMessage(phoneNumberId, id, from, Kind.OPTION, reply.path("title").asText(null),
                reply.path("id").asText(), sentAt);
    }

    private LocalDateTime sentAt(String epochSeconds) {
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(epochSeconds)), zone);
        } catch (NumberFormatException missing) {
            return null;
        }
    }
}
