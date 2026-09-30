package com.jclinical.automation.infra.adapters.out.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.ports.out.WhatsAppSenderPort.Credentials;
import com.jclinical.automation.domain.ports.out.WhatsAppSenderPort.SendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Formato de envio a la API de WhatsApp Cloud: texto; hasta 3 opciones como botones; de 4 a 10 como
 * lista; plantilla fuera de la ventana. Los textos se recortan a los limites de Meta y las fallas se
 * clasifican en reintentables (429, 5xx, red) o definitivas (otros 4xx), sin exponer el token.
 */
class MetaWhatsAppSenderTest {

    private static final String URL = "https://graph.facebook.com/v23.0/106540352242922/messages";
    private static final String TOKEN = "EAAtokenDeLaClinica1234";
    private static final String OK = "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.OUT1\"}]}";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Credentials credentials = new Credentials("106540352242922", TOKEN);
    private final AtomicReference<String> body = new AtomicReference<>();

    private MockRestServiceServer server;
    private MetaWhatsAppSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new MetaWhatsAppSender(builder.build(), objectMapper, "https://graph.facebook.com", "v23.0");
    }

    @Test
    void plainTextGoesAsATextMessage() throws Exception {
        expectSend();

        SendResult result = sender.sendMessage(credentials, "5215512345678", OutboundReply.text("Hola, Ana."));

        assertTrue(result.sent());
        assertEquals("wamid.OUT1", result.waMessageId());
        JsonNode sent = objectMapper.readTree(body.get());
        assertEquals("whatsapp", sent.path("messaging_product").asText());
        assertEquals("5215512345678", sent.path("to").asText());
        assertEquals("text", sent.path("type").asText());
        assertEquals("Hola, Ana.", sent.at("/text/body").asText());
        server.verify();
    }

    @Test
    void upToThreeOptionsGoAsReplyButtons() throws Exception {
        expectSend();

        sender.sendMessage(credentials, "5215512345678", new OutboundReply("¿Con qué médico quieres tu cita?", List.of(
                new ConversationOption("action:last-doctor", "Con Dra. Beatriz Ramos Fernández"),
                new ConversationOption("action:show-doctors", "Ver médicos de la clínica"))));

        JsonNode sent = objectMapper.readTree(body.get());
        assertEquals("interactive", sent.path("type").asText());
        assertEquals("button", sent.at("/interactive/type").asText());
        assertEquals("¿Con qué médico quieres tu cita?", sent.at("/interactive/body/text").asText());
        JsonNode buttons = sent.at("/interactive/action/buttons");
        assertEquals(2, buttons.size());
        assertEquals("reply", buttons.get(0).path("type").asText());
        assertEquals("action:last-doctor", buttons.get(0).at("/reply/id").asText());
        assertTrue(buttons.get(0).at("/reply/title").asText().length() <= 20, "titulo de boton <= 20");
    }

    @Test
    void fourToTenOptionsGoAsAList() throws Exception {
        expectSend();
        List<ConversationOption> slots = IntStream.range(0, 6)
                .mapToObj(i -> new ConversationOption("slot:" + i, "Martes 29 de septiembre a las 1" + i + ":00 con la Dra. Ramos"))
                .toList();

        sender.sendMessage(credentials, "5215512345678", new OutboundReply("Estos son los horarios disponibles:", slots));

        JsonNode sent = objectMapper.readTree(body.get());
        assertEquals("list", sent.at("/interactive/type").asText());
        assertFalse(sent.at("/interactive/action/button").asText().isBlank());
        JsonNode rows = sent.at("/interactive/action/sections/0/rows");
        assertEquals(6, rows.size());
        assertEquals("slot:0", rows.get(0).path("id").asText());
        assertTrue(rows.get(0).path("title").asText().length() <= 24, "titulo de fila <= 24");
        assertTrue(rows.get(0).path("description").asText().length() <= 72, "descripcion <= 72");
    }

    @Test
    void aTemplateCarriesItsLanguageAndBodyParameters() throws Exception {
        expectSend();

        SendResult result = sender.sendTemplate(credentials, "5215512345678", "aviso_paciente", "es_MX",
                List.of("Tu cita con la Dra. Ramos quedó confirmada."));

        assertTrue(result.sent());
        JsonNode sent = objectMapper.readTree(body.get());
        assertEquals("template", sent.path("type").asText());
        assertEquals("aviso_paciente", sent.at("/template/name").asText());
        assertEquals("es_MX", sent.at("/template/language/code").asText());
        assertEquals("body", sent.at("/template/components/0/type").asText());
        assertEquals("Tu cita con la Dra. Ramos quedó confirmada.", sent.at("/template/components/0/parameters/0/text").asText());
    }

    @Test
    void aReminderTemplateCarriesOneQuickReplyPayloadPerButton() throws Exception {
        expectSend();

        sender.sendTemplate(credentials, "5215512345678", "recordatorio_cita", "es_MX", List.of("Ana", "Clínica Sonrisa"),
                List.of("recordatorio:confirmar:1", "recordatorio:cancelar:1", "recordatorio:reprogramar:1"));

        JsonNode components = objectMapper.readTree(body.get()).at("/template/components");
        assertEquals("body", components.path(0).path("type").asText());
        assertEquals("Clínica Sonrisa", components.path(0).at("/parameters/1/text").asText());
        for (int index = 0; index < 3; index++) {
            JsonNode button = components.path(index + 1);
            assertEquals("button", button.path("type").asText());
            assertEquals("quick_reply", button.path("sub_type").asText());
            assertEquals(String.valueOf(index), button.path("index").asText());
            assertEquals("payload", button.at("/parameters/0/type").asText());
        }
        assertEquals("recordatorio:cancelar:1", components.path(2).at("/parameters/0/payload").asText());
    }

    @Test
    void rateLimitsAndServerErrorsAreRetryable() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        server.expect(requestTo(URL)).andRespond(withException(new IOException("sin red")));

        assertTrue(sender.sendMessage(credentials, "521", OutboundReply.text("a")).retryable());
        assertTrue(sender.sendMessage(credentials, "521", OutboundReply.text("a")).retryable());
        assertTrue(sender.sendMessage(credentials, "521", OutboundReply.text("a")).retryable());
    }

    @Test
    void aRejectedMessageIsPermanentAndDoesNotLeakTheToken() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"message\":\"(#131030) Recipient phone number not in allowed list\",\"code\":131030}}"));

        SendResult result = sender.sendMessage(credentials, "521", OutboundReply.text("a"));

        assertFalse(result.sent());
        assertFalse(result.retryable());
        assertTrue(result.error().contains("400"), result.error());
        assertTrue(result.error().contains("131030"), "el codigo de Meta ayuda a diagnosticar: " + result.error());
        assertFalse(result.error().contains(TOKEN));
    }

    private void expectSend() {
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andExpect(request -> body.set(request.getBody().toString()))
                .andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));
    }
}
