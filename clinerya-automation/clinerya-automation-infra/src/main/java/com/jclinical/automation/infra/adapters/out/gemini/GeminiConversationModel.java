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
    private static final String BUBBLE_RULE = "\n\nSi quieres mandar varios mensajes de WhatsApp seguidos, "
            + "sepáralos con una línea que diga solo " + BUBBLE_SEPARATOR + ".";
    private static final String NOTE_PREFIX = "[Nota del sistema, el paciente no la ve] ";
    private static final TypeReference<Map<String, Object>> ARGUMENTS = new TypeReference<>() {};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final ChannelSettingsRepositoryPort settings;

    public GeminiConversationModel(RestClient restClient, ObjectMapper objectMapper, String baseUrl,
                                   ChannelSettingsRepositoryPort settings) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.settings = settings;
    }

    @Override
    public ModelStep next(UUID clinicId, String systemInstruction, List<AgentMessage> transcript, List<ToolSpec> tools) {
        ChannelSettings clinic = settings.findByClinicId(clinicId)
                .filter(found -> found.geminiApiKey() != null && !found.geminiApiKey().isBlank())
                .orElseThrow(() -> new IllegalStateException("La clínica no tiene configurada su clave de Gemini"));
        try {
            JsonNode response = restClient.post()
                    .uri(baseUrl + "/v1beta/models/" + clinic.geminiModel() + ":generateContent")
                    .header("x-goog-api-key", clinic.geminiApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json(request(systemInstruction, transcript, tools)))
                    .retrieve()
                    .body(JsonNode.class);
            return step(response);
        } catch (RestClientResponseException rejected) {
            // El cuerpo de error de Gemini explica la causa y nunca trae la clave (va en cabecera).
            log.warn(">>>> [AGENTE] Gemini respondio {} (modelo {}, clinica {}): {}", rejected.getStatusCode(),
                    clinic.geminiModel(), clinicId, abbreviate(rejected.getResponseBodyAsString()));
            throw rejected;
        } catch (RuntimeException failure) {
            log.warn(">>>> [AGENTE] Fallo la llamada a Gemini (modelo {}, clinica {}): {}", clinic.geminiModel(), clinicId,
                    failure.toString());
            throw failure;
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
        transcript.forEach(message -> contents.add(content(message)));
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
            throw new IllegalStateException("Gemini no devolvió contenido");
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
            throw new IllegalStateException("Gemini devolvió una respuesta vacía");
        }
        return new ModelStep.Reply(bubbles);
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
