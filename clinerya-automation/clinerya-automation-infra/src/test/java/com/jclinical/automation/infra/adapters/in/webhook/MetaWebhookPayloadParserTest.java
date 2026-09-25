package com.jclinical.automation.infra.adapters.in.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage.Kind;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lectura del JSON que Meta manda al webhook: texto, respuestas de botones y listas, y respuestas a
 * botones de plantilla. Lo demas (audio, imagen) se marca como no soportado; los avisos de estado no
 * son mensajes.
 */
class MetaWebhookPayloadParserTest {

    private final MetaWebhookPayloadParser parser = new MetaWebhookPayloadParser(new ObjectMapper(), ZoneOffset.UTC);

    @Test
    void aTextMessage() {
        List<WhatsAppInboundMessage> messages = parse(envelope("""
                {"from":"5215512345678","id":"wamid.AAA","timestamp":"1790330400","type":"text","text":{"body":"Hola, quiero una cita"}}
                """));

        assertEquals(List.of(new WhatsAppInboundMessage("106540352242922", "wamid.AAA", "5215512345678", Kind.TEXT,
                "Hola, quiero una cita", null, LocalDateTime.of(2026, 9, 25, 10, 0))), messages);
    }

    @Test
    void aReplyButtonCarriesTheChosenOption() {
        WhatsAppInboundMessage message = parse(envelope("""
                {"from":"5215512345678","id":"wamid.B","timestamp":"1790330400","type":"interactive",
                 "interactive":{"type":"button_reply","button_reply":{"id":"action:book","title":"Agendar una cita"}}}
                """)).get(0);

        assertEquals(Kind.OPTION, message.kind());
        assertEquals("action:book", message.selectedOptionId());
        assertEquals("Agendar una cita", message.text());
    }

    @Test
    void aListRowCarriesTheChosenOption() {
        WhatsAppInboundMessage message = parse(envelope("""
                {"from":"5215512345678","id":"wamid.C","timestamp":"1790330400","type":"interactive",
                 "interactive":{"type":"list_reply","list_reply":{"id":"slot:2026-09-29T10:00|2026-09-29T10:30","title":"Mar 29/09 10:00"}}}
                """)).get(0);

        assertEquals(Kind.OPTION, message.kind());
        assertEquals("slot:2026-09-29T10:00|2026-09-29T10:30", message.selectedOptionId());
    }

    @Test
    void aTemplateQuickReplyCarriesItsPayload() {
        WhatsAppInboundMessage message = parse(envelope("""
                {"from":"5215512345678","id":"wamid.D","timestamp":"1790330400","type":"button",
                 "button":{"payload":"action:book","text":"Agendar"}}
                """)).get(0);

        assertEquals(Kind.OPTION, message.kind());
        assertEquals("action:book", message.selectedOptionId());
    }

    @Test
    void mediaIsMarkedUnsupported() {
        WhatsAppInboundMessage message = parse(envelope("""
                {"from":"5215512345678","id":"wamid.E","timestamp":"1790330400","type":"image","image":{"id":"123"}}
                """)).get(0);

        assertEquals(Kind.UNSUPPORTED, message.kind());
        assertEquals("wamid.E", message.waMessageId());
    }

    @Test
    void statusUpdatesAreNotMessages() {
        String statuses = """
                {"object":"whatsapp_business_account","entry":[{"id":"102290129340398","changes":[{"field":"messages",
                 "value":{"messaging_product":"whatsapp","metadata":{"phone_number_id":"106540352242922"},
                 "statuses":[{"id":"wamid.X","status":"delivered","timestamp":"1790330400","recipient_id":"5215512345678"}]}}]}]}
                """;

        assertTrue(parse(statuses).isEmpty());
    }

    @Test
    void aBodyThatIsNotJsonMeansNoMessages() {
        assertTrue(parser.parse("no es json".getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertTrue(parser.parse(new byte[0]).isEmpty());
    }

    @Test
    void everyEntryAndChangeIsRead() {
        String two = """
                {"object":"whatsapp_business_account","entry":[
                 {"changes":[{"value":{"metadata":{"phone_number_id":"111"},"messages":[
                   {"from":"521","id":"wamid.1","timestamp":"1790330400","type":"text","text":{"body":"a"}}]}}]},
                 {"changes":[{"value":{"metadata":{"phone_number_id":"222"},"messages":[
                   {"from":"522","id":"wamid.2","timestamp":"1790330400","type":"text","text":{"body":"b"}}]}}]}]}
                """;

        List<WhatsAppInboundMessage> messages = parse(two);

        assertEquals(List.of("111", "222"), messages.stream().map(WhatsAppInboundMessage::phoneNumberId).toList());
    }

    private List<WhatsAppInboundMessage> parse(String json) {
        return parser.parse(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String envelope(String message) {
        return """
                {"object":"whatsapp_business_account","entry":[{"id":"102290129340398","changes":[{"field":"messages",
                 "value":{"messaging_product":"whatsapp",
                 "metadata":{"display_phone_number":"525512345678","phone_number_id":"106540352242922"},
                 "contacts":[{"profile":{"name":"Ana"},"wa_id":"5215512345678"}],
                 "messages":[%s]}}]}]}
                """.formatted(message);
    }
}
