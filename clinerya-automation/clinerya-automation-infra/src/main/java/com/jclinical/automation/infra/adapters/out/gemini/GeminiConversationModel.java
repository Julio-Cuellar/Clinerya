package com.jclinical.automation.infra.adapters.out.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jclinical.automation.domain.agent.AgentMessage;
import com.jclinical.automation.domain.agent.ConversationModelPort;
import com.jclinical.automation.domain.agent.ModelStep;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * El modelo de la conversacion con Gemini (function calling):
 * <ul>
 *   <li>Viaja el chat, las llamadas y resultados de herramientas del turno y las notas internas del
 *       codigo; las herramientas se declaran con su esquema.</li>
 *   <li>Cada clinica usa su clave (en la cabecera x-goog-api-key, nunca en la URL) y su modelo.</li>
 *   <li>Varios mensajes de WhatsApp se piden separados por una linea con {@link #BUBBLE_SEPARATOR}.</li>
 *   <li>Cualquier falla se propaga: el agente la reintenta y, si sigue, pasa el chat a una persona.</li>
 * </ul>
 */
@Slf4j
public class GeminiConversationModel implements ConversationModelPort {

    public static final String BUBBLE_SEPARATOR = "---";

    /** Gemini firma su razonamiento en la llamada; se guarda aparte de los argumentos y se le devuelve. */
    static final String THOUGHT_SIGNATURE = "__thoughtSignature";

    private static final double TEMPERATURE = 0.7;
    private static final int MAX_LOGGED_ERROR = 1000;
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(1);
    private static final int TOO_MANY_REQUESTS = 429;
    private static final String BUBBLE_RULE = "\n\nSi quieres mandar varios mensajes de WhatsApp seguidos, "
            + "sepáralos con una línea que diga solo " + BUBBLE_SEPARATOR + ".";
    private static final String NOTE_PREFIX = "[Nota del sistema, el paciente no la ve] ";
    private static final TypeReference<Map<String, Object>> ARGUMENTS = new TypeReference<>() {};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final ChannelSettingsRepositoryPort settings;
    private final Duration retryDelay;

    public GeminiConversationModel(RestClient restClient, ObjectMapper objectMapper, String baseUrl,
                                   ChannelSettingsRepositoryPort settings) {
        this(restClient, objectMapper, baseUrl, settings, DEFAULT_RETRY_DELAY);
    }

    /** {@code retryDelay}: espera antes de reintentar cuando Gemini esta saturado (429) o falla del lado del servidor. */
    public GeminiConversationModel(RestClient restClient, ObjectMapper objectMapper, String baseUrl,
                                   ChannelSettingsRepositoryPort settings, Duration retryDelay) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.settings = settings;
        this.retryDelay = retryDelay;
    }

    @Override
    public ModelStep next(UUID clinicId, String systemInstruction, List<AgentMessage> transcript, List<ToolSpec> tools) {
        ChannelSettings clinic = settings.findByClinicId(clinicId)
                .filter(found -> found.geminiApiKey() != null && !found.geminiApiKey().isBlank())
                .orElseThrow(() -> new IllegalStateException("La clínica no tiene configurada su clave de Gemini"));
        String body = json(request(systemInstruction, transcript, tools));
        try {
            return step(call(clinic, clinicId, body));
        } catch (RuntimeException failure) {
            log.warn(">>>> [AGENTE] Fallo la llamada a Gemini (modelo {}, clinica {}): {}", clinic.geminiModel(), clinicId,
                    failure.toString());
            throw failure;
        }
    }

    /** Una sola llamada, con un reintento si Gemini esta saturado o falla de su lado; un rechazo (4xx) no se repite. */
    private JsonNode call(ChannelSettings clinic, UUID clinicId, String body) {
        try {
            return post(clinic, body);
        } catch (RestClientResponseException rejected) {
            // El cuerpo de error de Gemini explica la causa y nunca trae la clave (va en cabecera).
            log.warn(">>>> [AGENTE] Gemini respondio {} (modelo {}, clinica {}): {}", rejected.getStatusCode(),
                    clinic.geminiModel(), clinicId, abbreviate(rejected.getResponseBodyAsString()));
            if (!retryable(rejected)) {
                throw rejected;
            }
            pause();
            return post(clinic, body);
        }
    }

    private JsonNode post(ChannelSettings clinic, String body) {
        return restClient.post()
                .uri(baseUrl + "/v1beta/models/" + clinic.geminiModel() + ":generateContent")
                .header("x-goog-api-key", clinic.geminiApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    private static boolean retryable(RestClientResponseException rejected) {
        return rejected.getStatusCode().value() == TOO_MANY_REQUESTS || rejected.getStatusCode().is5xxServerError();
    }

    private void pause() {
        if (retryDelay.isZero()) {
            return;
        }
        try {
            Thread.sleep(retryDelay.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpio la espera para reintentar con Gemini", interrupted);
        }
    }

    private static String abbreviate(String body) {
        return body == null || body.length() <= MAX_LOGGED_ERROR ? body : body.substring(0, MAX_LOGGED_ERROR) + "...";
    }

    private ObjectNode request(String systemInstruction, List<AgentMessage> transcript, List<ToolSpec> tools) {
        ObjectNode request = objectMapper.createObjectNode();
        request.putObject("systemInstruction").putArray("parts").addObject()
                .put("text", systemInstruction + BUBBLE_RULE);
        ArrayNode contents = request.putArray("contents");
        ObjectNode previous = null;
        for (AgentMessage message : transcript) {
            ObjectNode content = content(message);
            // Gemini espera turnos que alternan: las llamadas paralelas van en un turno del modelo, sus resultados
            // (y las notas internas) en uno solo del usuario.
            if (previous != null && previous.get("role").asText().equals(content.get("role").asText())) {
                ((ArrayNode) previous.get("parts")).addAll((ArrayNode) content.get("parts"));
            } else {
                contents.add(content);
                previous = content;
            }
        }
        if (!tools.isEmpty()) {
            ArrayNode declarations = request.putArray("tools").addObject().putArray("functionDeclarations");
            tools.forEach(tool -> declarations.add(declaration(tool)));
        }
        request.putObject("generationConfig").put("temperature", TEMPERATURE);
        return request;
    }

    private ObjectNode content(AgentMessage message) {
        ObjectNode content = objectMapper.createObjectNode();
        ObjectNode part = content.putArray("parts").addObject();
        switch (message) {
            case AgentMessage.User user -> {
                content.put("role", "user");
                part.put("text", user.text());
            }
            case AgentMessage.Assistant assistant -> {
                content.put("role", "model");
                part.put("text", assistant.text());
            }
            case AgentMessage.Note note -> {
                content.put("role", "user");
                part.put("text", NOTE_PREFIX + note.text());
            }
            case AgentMessage.ToolCall call -> {
                content.put("role", "model");
                Map<String, Object> arguments = new HashMap<>(call.arguments());
                Object signature = arguments.remove(THOUGHT_SIGNATURE);
                ObjectNode functionCall = part.putObject("functionCall");
                functionCall.put("name", call.name());
                functionCall.set("args", objectMapper.valueToTree(arguments));
                if (signature != null) {
                    part.put("thoughtSignature", signature.toString());
                }
            }
            case AgentMessage.ToolResult result -> {
                content.put("role", "user");
                ObjectNode functionResponse = part.putObject("functionResponse");
                functionResponse.put("name", result.name());
                functionResponse.set("response", objectMapper.valueToTree(result.content()));
            }
        }
        return content;
    }

    private ObjectNode declaration(ToolSpec tool) {
        ObjectNode declaration = objectMapper.createObjectNode();
        declaration.put("name", tool.name());
        declaration.put("description", tool.description());
        if (tool.parameters().isEmpty()) {
            return declaration;
        }
        ObjectNode parameters = declaration.putObject("parameters");
        parameters.put("type", "OBJECT");
        ObjectNode properties = parameters.putObject("properties");
        ArrayNode required = objectMapper.createArrayNode();
        for (ToolSpec.Parameter parameter : tool.parameters()) {
            properties.putObject(parameter.name())
                    .put("type", parameter.type().toUpperCase(Locale.ROOT))
                    .put("description", parameter.description());
            if (parameter.required()) {
                required.add(parameter.name());
            }
        }
        if (!required.isEmpty()) {
            parameters.set("required", required);
        }
        return declaration;
    }

    private ModelStep step(JsonNode response) {
        JsonNode parts = response == null ? null : response.at("/candidates/0/content/parts");
        if (parts == null || !parts.isArray() || parts.isEmpty()) {
            throw new IllegalStateException("Gemini no devolvió contenido (" + stopReason(response) + ")");
        }
        List<AgentMessage.ToolCall> calls = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("functionCall")) {
                calls.add(toolCall(part));
            } else if (part.has("text") && !part.path("thought").asBoolean(false)) {
                text.append(part.get("text").asText());
            }
        }
        if (!calls.isEmpty()) {
            return new ModelStep.CallTools(calls);
        }
        List<String> bubbles = bubbles(text.toString());
        if (bubbles.isEmpty()) {
            throw new IllegalStateException("Gemini devolvió una respuesta vacía (" + stopReason(response) + ")");
        }
        return new ModelStep.Reply(bubbles);
    }

    /** Por que Gemini no dio contenido: lo bloqueo (blockReason) o se detuvo (finishReason). */
    private static String stopReason(JsonNode response) {
        if (response == null) {
            return "sin cuerpo";
        }
        String blocked = response.at("/promptFeedback/blockReason").asText("");
        if (!blocked.isBlank()) {
            return "blockReason " + blocked;
        }
        String finish = response.at("/candidates/0/finishReason").asText("");
        return finish.isBlank() ? "sin finishReason" : "finishReason " + finish;
    }

    private AgentMessage.ToolCall toolCall(JsonNode part) {
        JsonNode call = part.get("functionCall");
        Map<String, Object> arguments = new HashMap<>();
        if (call.path("args").isObject()) {
            arguments.putAll(objectMapper.convertValue(call.get("args"), ARGUMENTS));
        }
        if (part.hasNonNull("thoughtSignature")) {
            arguments.put(THOUGHT_SIGNATURE, part.get("thoughtSignature").asText());
        }
        String id = call.hasNonNull("id") ? call.get("id").asText() : UUID.randomUUID().toString();
        return new AgentMessage.ToolCall(id, call.path("name").asText(), arguments);
    }

    private static List<String> bubbles(String text) {
        return Arrays.stream(text.split("\\R\\s*" + BUBBLE_SEPARATOR + "\\s*(\\R|$)"))
                .map(String::strip)
                .filter(bubble -> !bubble.isEmpty())
                .toList();
    }

    private String json(ObjectNode request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
