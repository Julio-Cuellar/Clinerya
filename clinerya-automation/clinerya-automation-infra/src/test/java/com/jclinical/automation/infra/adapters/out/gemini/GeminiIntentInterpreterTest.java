package com.jclinical.automation.infra.adapters.out.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * El interprete solo traduce texto libre a una de las opciones ofrecidas. A Gemini viaja el texto y
 * las etiquetas de las opciones, nada del expediente; la respuesta queda restringida por esquema a
 * esos ids (o NONE) y cualquier falla se trata como "no entendi".
 */
class GeminiIntentInterpreterTest {

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<ConversationOption> options = List.of(
            new ConversationOption("action:last-doctor", "Con Dra. Beatriz Ramos"),
            new ConversationOption("action:show-doctors", "Ver médicos de la clínica"));

    private final UUID clinicId = UUID.randomUUID();
    private final Map<UUID, ChannelSettings> settings = new HashMap<>();

    private MockRestServiceServer server;
    private GeminiIntentInterpreter interpreter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        settings.put(clinicId, ChannelSettings.unconfigured(clinicId).toBuilder().geminiApiKey("clave-de-prueba").build());
        interpreter = new GeminiIntentInterpreter(builder.build(), objectMapper,
                "https://generativelanguage.googleapis.com", new SettingsById(settings));
    }

    @Test
    void returnsTheOptionGeminiChose() {
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(answer("action:last-doctor"), MediaType.APPLICATION_JSON));

        Optional<String> chosen = interpreter.interpret(clinicId, "con la doctora de siempre", options);

        assertEquals(Optional.of("action:last-doctor"), chosen);
        server.verify();
    }

    @Test
    void sendsOnlyTheTextAndTheOfferedOptionsConstrainedBySchema() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        server.expect(requestTo(URL))
                .andExpect(header("x-goog-api-key", "clave-de-prueba"))
                .andExpect(request -> body.set(request.getBody().toString()))
                .andRespond(withSuccess(answer("NONE"), MediaType.APPLICATION_JSON));

        interpreter.interpret(clinicId, "con la doctora de siempre", options);

        JsonNode request = objectMapper.readTree(body.get());
        String userPart = request.at("/contents/0/parts/0/text").asText();
        assertTrue(userPart.contains("con la doctora de siempre"), userPart);
        assertTrue(userPart.contains("Ver médicos de la clínica"), userPart);
        assertEquals(0, request.at("/generationConfig/temperature").asInt());
        JsonNode allowed = request.at("/generationConfig/responseSchema/properties/optionId/enum");
        assertEquals(List.of("action:last-doctor", "action:show-doctors", "NONE"),
                objectMapper.convertValue(allowed, List.class));
        assertFalse(body.get().contains("clave-de-prueba"), "la clave nunca viaja en el cuerpo");
    }

    @Test
    void noneMeansNotUnderstood() {
        server.expect(requestTo(URL)).andRespond(withSuccess(answer("NONE"), MediaType.APPLICATION_JSON));

        assertTrue(interpreter.interpret(clinicId, "asdf", options).isEmpty());
    }

    @Test
    void aMalformedAnswerMeansNotUnderstood() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

        assertTrue(interpreter.interpret(clinicId, "con la doctora", options).isEmpty());
    }

    @Test
    void aServerErrorMeansNotUnderstood() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertTrue(interpreter.interpret(clinicId, "con la doctora", options).isEmpty());
    }

    @Test
    void blankTextDoesNotCallGemini() {
        assertTrue(interpreter.interpret(clinicId, "   ", options).isEmpty());
        server.verify();
    }

    @Test
    void aClinicWithoutItsOwnGeminiKeyNeverCallsGemini() {
        UUID withoutKey = UUID.randomUUID();
        settings.put(withoutKey, ChannelSettings.unconfigured(withoutKey));

        assertTrue(interpreter.interpret(withoutKey, "con la doctora", options).isEmpty());
        assertTrue(interpreter.interpret(UUID.randomUUID(), "con la doctora", options).isEmpty(), "sin configuracion");
        server.verify();
    }

    @Test
    void eachClinicUsesItsOwnKeyAndModel() {
        UUID otherClinic = UUID.randomUUID();
        settings.put(otherClinic, ChannelSettings.unconfigured(otherClinic).toBuilder()
                .geminiApiKey("clave-de-otra-clinica").geminiModel("gemini-2.5-pro").build());
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent"))
                .andExpect(header("x-goog-api-key", "clave-de-otra-clinica"))
                .andRespond(withSuccess(answer("action:show-doctors"), MediaType.APPLICATION_JSON));

        assertEquals(Optional.of("action:show-doctors"), interpreter.interpret(otherClinic, "ver doctores", options));
        server.verify();
    }

    /** Lectura de la configuracion por clinica, como la hace el repositorio real. */
    record SettingsById(Map<UUID, ChannelSettings> byClinic) implements ChannelSettingsRepositoryPort {
        @Override
        public Optional<ChannelSettings> findByClinicId(UUID clinicId) {
            return Optional.ofNullable(byClinic.get(clinicId));
        }

        @Override
        public Optional<ChannelSettings> findByPhoneNumberId(String phoneNumberId) {
            return Optional.empty();
        }

        @Override
        public Optional<ChannelSettings> findByWebhookKey(String webhookKey) {
            return Optional.empty();
        }

        @Override
        public ChannelSettings save(ChannelSettings value) {
            byClinic.put(value.clinicId(), value);
            return value;
        }
    }

    private String answer(String optionId) {
        String inner = "{\\\"optionId\\\":\\\"" + optionId + "\\\"}";
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + inner + "\"}]}}]}";
    }
}
