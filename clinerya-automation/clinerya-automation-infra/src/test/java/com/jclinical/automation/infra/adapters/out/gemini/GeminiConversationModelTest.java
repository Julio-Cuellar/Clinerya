package com.jclinical.automation.infra.adapters.out.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.agent.AgentMessage;
import com.jclinical.automation.domain.agent.ModelStep;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.infra.adapters.out.gemini.GeminiIntentInterpreterTest.SettingsById;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * El modelo de la conversacion con Gemini (function calling). Viaja el historial del chat, las
 * llamadas y resultados de herramientas del turno y las notas internas; las herramientas se declaran
 * con su esquema. Cada clinica usa su clave (en cabecera) y su modelo. Una falla se reporta como
 * excepcion: el agente la reintenta y, si sigue, pasa el chat a una persona.
 */
class GeminiConversationModelTest {

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID clinicId = UUID.randomUUID();
    private final Map<UUID, ChannelSettings> settings = new HashMap<>();
    private MockRestServiceServer server;
    private GeminiConversationModel model;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        settings.put(clinicId, ChannelSettings.unconfigured(clinicId).toBuilder().geminiApiKey("clave-de-prueba").build());
        model = new GeminiConversationModel(builder.build(), objectMapper, "https://generativelanguage.googleapis.com",
                new SettingsById(settings), Duration.ZERO);
    }

    @Test
    void theChatTheToolsAndTheNotesTravelInGeminiFormat() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        server.expect(requestTo(URL)).andExpect(header("x-goog-api-key", "clave-de-prueba"))
                .andExpect(request -> body.set(request.getBody().toString()))
                .andRespond(withSuccess(text("Hola"), MediaType.APPLICATION_JSON));
        List<AgentMessage> transcript = List.of(
                new AgentMessage.User("hola"),
                new AgentMessage.Assistant("¡Hola! ¿En qué te ayudo?"),
                new AgentMessage.ToolCall("c1", "info_clinica", Map.of()),
                new AgentMessage.ToolResult("c1", "info_clinica", Map.of("nombre", "Clínica Sonrisa")),
                new AgentMessage.Note("Corrige el precio."));
        List<ToolSpec> tools = List.of(new ToolSpec("buscar_horarios", "Busca horarios",
                List.of(new ToolSpec.Parameter("dias", "integer", "Días", false),
                        new ToolSpec.Parameter("medico", "string", "Médico", true))),
                new ToolSpec("info_clinica", "Datos de la clínica", List.of()));

        model.next(clinicId, "Eres la recepción de Clínica Sonrisa.", transcript, tools);

        JsonNode request = objectMapper.readTree(body.get());
        String system = request.at("/systemInstruction/parts/0/text").asText();
        assertTrue(system.startsWith("Eres la recepción de Clínica Sonrisa."), system);
        assertTrue(system.contains(GeminiConversationModel.BUBBLE_SEPARATOR), system);
        JsonNode contents = request.get("contents");
        assertEquals("user", contents.get(0).get("role").asText());
        assertEquals("hola", contents.get(0).at("/parts/0/text").asText());
        assertEquals("model", contents.get(1).get("role").asText());
        assertEquals("info_clinica", contents.get(2).at("/parts/0/functionCall/name").asText());
        assertEquals("Clínica Sonrisa", contents.get(3).at("/parts/0/functionResponse/response/nombre").asText());
        assertTrue(contents.get(4).at("/parts/0/text").asText().contains("Corrige el precio."));
        JsonNode declaration = request.at("/tools/0/functionDeclarations/0");
        assertEquals("buscar_horarios", declaration.get("name").asText());
        assertEquals("INTEGER", declaration.at("/parameters/properties/dias/type").asText());
        assertEquals(List.of("medico"), objectMapper.convertValue(declaration.at("/parameters/required"), List.class));
        assertTrue(request.at("/tools/0/functionDeclarations/1/parameters").isMissingNode(), "sin parametros no se declara esquema");
        assertTrue(request.at("/generationConfig/temperature").asDouble() > 0, "la redaccion varia; la guarda protege los datos");
        assertFalse(body.get().contains("clave-de-prueba"), "la clave nunca viaja en el cuerpo");
    }

    @Test
    void aTextAnswerIsSplitIntoBubblesByTheSeparator() {
        server.expect(requestTo(URL)).andRespond(withSuccess(text("¡Hola!\n---\nEstamos en el centro."), MediaType.APPLICATION_JSON));

        ModelStep step = model.next(clinicId, "instrucciones", List.of(new AgentMessage.User("hola")), List.of());

        assertEquals(new ModelStep.Reply(List.of("¡Hola!", "Estamos en el centro.")), step);
    }

    @Test
    void functionCallsBecomeToolCallsWithTheirArguments() {
        String response = "{\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":["
                + "{\"functionCall\":{\"name\":\"buscar_horarios\",\"args\":{\"dias\":3,\"turno\":\"tarde\"}}}]}}]}";
        server.expect(requestTo(URL)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        ModelStep step = model.next(clinicId, "instrucciones", List.of(new AgentMessage.User("el jueves en la tarde")), List.of());

        ModelStep.CallTools calls = (ModelStep.CallTools) step;
        AgentMessage.ToolCall call = calls.calls().getFirst();
        assertEquals("buscar_horarios", call.name());
        assertEquals(3, ((Number) call.arguments().get("dias")).intValue());
        assertEquals("tarde", call.arguments().get("turno"));
        assertFalse(call.id().isBlank());
    }

    @Test
    void aServerErrorThatPersistsIsReportedToTheAgent() {
        server.expect(ExpectedCount.twice(), requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(RuntimeException.class,
                () -> model.next(clinicId, "instrucciones", List.of(new AgentMessage.User("hola")), List.of()));
        server.verify();
    }

    @Test
    void aBusyOrOverloadedGeminiIsRetriedOnce() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(URL)).andRespond(withSuccess(text("¡Hola!"), MediaType.APPLICATION_JSON));

        ModelStep step = model.next(clinicId, "instrucciones", List.of(new AgentMessage.User("hola")), List.of());

        assertEquals(new ModelStep.Reply(List.of("¡Hola!")), step);
    }

    @Test
    void aRejectedRequestIsNotRetried() {
        server.expect(ExpectedCount.once(), requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(RuntimeException.class,
                () -> model.next(clinicId, "instrucciones", List.of(new AgentMessage.User("hola")), List.of()));
        server.verify();
    }

    @Test
    void anAnswerWithoutContentSaysWhyGeminiStopped() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"candidates\":[{\"finishReason\":\"MALFORMED_FUNCTION_CALL\"}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}", MediaType.APPLICATION_JSON));

        IllegalStateException malformed = assertThrows(IllegalStateException.class,
                () -> model.next(clinicId, "instrucciones", List.of(new AgentMessage.User("hola")), List.of()));
        IllegalStateException blocked = assertThrows(IllegalStateException.class,
                () -> model.next(clinicId, "instrucciones", List.of(new AgentMessage.User("hola")), List.of()));

        assertTrue(malformed.getMessage().contains("MALFORMED_FUNCTION_CALL"), malformed.getMessage());
        assertTrue(blocked.getMessage().contains("SAFETY"), blocked.getMessage());
    }

    @Test
    void aClinicWithoutItsOwnKeyNeverCallsGemini() {
        UUID withoutKey = UUID.randomUUID();
        settings.put(withoutKey, ChannelSettings.unconfigured(withoutKey));

        assertThrows(IllegalStateException.class,
                () -> model.next(withoutKey, "instrucciones", List.of(new AgentMessage.User("hola")), List.of()));
        server.verify();
    }

    private String text(String answer) throws RuntimeException {
        try {
            return "{\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":[{\"text\":"
                    + objectMapper.writeValueAsString(answer) + "}]}}]}";
        } catch (com.fasterxml.jackson.core.JsonProcessingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
