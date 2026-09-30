package com.jclinical.automation.infra.adapters.out.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.ports.out.WhatsAppSenderPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Envio por la API de WhatsApp Cloud. Hasta 3 opciones van como botones y de 4 a 10 como lista; los
 * textos se recortan a los limites de Meta. El error que se guarda lleva el estado HTTP y el codigo
 * de Meta, nunca el cuerpo completo (podria traer datos del destinatario o el token).
 */
public class MetaWhatsAppSender implements WhatsAppSenderPort {

    static final int MAX_BUTTONS = 3;
    static final int MAX_ROWS = 10;
    static final int BUTTON_TITLE_MAX = 20;
    static final int ROW_TITLE_MAX = 24;
    static final int ROW_DESCRIPTION_MAX = 72;
    static final int INTERACTIVE_BODY_MAX = 1024;
    static final int TEXT_BODY_MAX = 4096;
    static final String LIST_BUTTON_LABEL = "Ver opciones";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String graphBaseUrl;
    private final String graphVersion;

    public MetaWhatsAppSender(RestClient restClient, ObjectMapper objectMapper, String graphBaseUrl, String graphVersion) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.graphBaseUrl = graphBaseUrl;
        this.graphVersion = graphVersion;
    }

    @Override
    public SendResult sendMessage(Credentials credentials, String to, OutboundReply reply) {
        ObjectNode payload = envelope(to);
        List<ConversationOption> options = reply.options();
        if (options.isEmpty()) {
            payload.put("type", "text");
            ObjectNode text = payload.putObject("text");
            text.put("preview_url", false);
            text.put("body", truncate(reply.text(), TEXT_BODY_MAX));
        } else if (options.size() <= MAX_BUTTONS) {
            buttons(payload, reply);
        } else {
            list(payload, reply);
        }
        return post(credentials, payload);
    }

    @Override
    public SendResult sendTemplate(Credentials credentials, String to, String templateName, String languageCode,
                                   List<String> parameters) {
        return sendTemplate(credentials, to, templateName, languageCode, parameters, List.of());
    }

    /** Cada boton de respuesta rapida de la plantilla lleva su payload, en el orden en que se aprobo en Meta. */
    @Override
    public SendResult sendTemplate(Credentials credentials, String to, String templateName, String languageCode,
                                   List<String> parameters, List<String> buttonPayloads) {
        ObjectNode payload = envelope(to);
        payload.put("type", "template");
        ObjectNode template = payload.putObject("template");
        template.put("name", templateName);
        template.putObject("language").put("code", languageCode);
        if (parameters.isEmpty() && buttonPayloads.isEmpty()) {
            return post(credentials, payload);
        }
        ArrayNode components = template.putArray("components");
        if (!parameters.isEmpty()) {
            ObjectNode body = components.addObject();
            body.put("type", "body");
            ArrayNode values = body.putArray("parameters");
            parameters.forEach(value -> values.addObject().put("type", "text").put("text", value));
        }
        for (int index = 0; index < buttonPayloads.size(); index++) {
            ObjectNode button = components.addObject();
            button.put("type", "button");
            button.put("sub_type", "quick_reply");
            button.put("index", String.valueOf(index));
            button.putArray("parameters").addObject().put("type", "payload").put("payload", buttonPayloads.get(index));
        }
        return post(credentials, payload);
    }

    private ObjectNode envelope(String to) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", to);
        return payload;
    }

    private void buttons(ObjectNode payload, OutboundReply reply) {
        ObjectNode interactive = interactive(payload, "button", reply.text());
        ArrayNode buttons = interactive.putObject("action").putArray("buttons");
        for (ConversationOption option : reply.options()) {
            ObjectNode button = buttons.addObject();
            button.put("type", "reply");
            button.putObject("reply").put("id", option.id()).put("title", truncate(option.label(), BUTTON_TITLE_MAX));
        }
    }

    private void list(ObjectNode payload, OutboundReply reply) {
        ObjectNode interactive = interactive(payload, "list", reply.text());
        ObjectNode action = interactive.putObject("action");
        action.put("button", LIST_BUTTON_LABEL);
        ArrayNode rows = action.putArray("sections").addObject().put("title", "Opciones").putArray("rows");
        for (ConversationOption option : reply.options().stream().limit(MAX_ROWS).toList()) {
            ObjectNode row = rows.addObject();
            row.put("id", option.id());
            row.put("title", truncate(option.label(), ROW_TITLE_MAX));
            if (option.label().length() > ROW_TITLE_MAX) {
                row.put("description", truncate(option.label(), ROW_DESCRIPTION_MAX));
            }
        }
    }

    private static ObjectNode interactive(ObjectNode payload, String type, String text) {
        payload.put("type", "interactive");
        ObjectNode interactive = payload.putObject("interactive");
        interactive.put("type", type);
        interactive.putObject("body").put("text", truncate(text, INTERACTIVE_BODY_MAX));
        return interactive;
    }

    private SendResult post(Credentials credentials, ObjectNode payload) {
        try {
            String response = restClient.post()
                    .uri(graphBaseUrl + "/" + graphVersion + "/" + credentials.phoneNumberId() + "/messages")
                    .header("Authorization", "Bearer " + credentials.accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(payload))
                    .retrieve()
                    .body(String.class);
            String id = objectMapper.readTree(response == null ? "{}" : response).at("/messages/0/id").asText("");
            return id.isBlank() ? SendResult.failed(true, "Meta no devolvió el id del mensaje.") : SendResult.ok(id);
        } catch (RestClientResponseException rejected) {
            int status = rejected.getStatusCode().value();
            boolean retryable = status == 429 || status >= 500;
            return SendResult.failed(retryable, "HTTP " + status + metaCode(rejected.getResponseBodyAsString()));
        } catch (RestClientException | JsonProcessingException unreachable) {
            return SendResult.failed(true, "No se pudo conectar con Meta.");
        }
    }

    private String metaCode(String body) {
        try {
            JsonNode code = objectMapper.readTree(body == null || body.isBlank() ? "{}" : body).at("/error/code");
            return code.isMissingNode() || code.isNull() ? "" : " (código de Meta " + code.asText() + ")";
        } catch (JsonProcessingException unreadable) {
            return "";
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
