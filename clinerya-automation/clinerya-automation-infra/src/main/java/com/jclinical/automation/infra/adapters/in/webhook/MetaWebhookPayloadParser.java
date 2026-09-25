package com.jclinical.automation.infra.adapters.in.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage.Kind;
import com.jclinical.automation.domain.ports.out.WebhookPayloadParserPort;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

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
    public List<WhatsAppInboundMessage> parse(byte[] rawBody) {
        if (rawBody == null || rawBody.length == 0) {
            return List.of();
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (IOException unreadable) {
            return List.of();
        }
        if (root == null) {
            return List.of();
        }
        List<WhatsAppInboundMessage> messages = new ArrayList<>();
        for (JsonNode entry : root.path("entry")) {
            for (JsonNode change : entry.path("changes")) {
                JsonNode value = change.path("value");
                String phoneNumberId = value.path("metadata").path("phone_number_id").asText(null);
                for (JsonNode message : value.path("messages")) {
                    messages.add(toMessage(phoneNumberId, message));
                }
            }
        }
        return List.copyOf(messages);
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
