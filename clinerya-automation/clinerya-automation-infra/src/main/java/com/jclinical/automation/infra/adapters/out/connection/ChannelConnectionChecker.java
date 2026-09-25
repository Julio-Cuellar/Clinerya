package com.jclinical.automation.infra.adapters.out.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.ports.out.ChannelConnectionCheckPort;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Prueba de solo lectura de las credenciales de una clinica: consulta el numero en la Graph API de
 * Meta y el modelo en Gemini. Los mensajes de error se arman aqui, nunca con el cuerpo que responde
 * el proveedor, para que un secreto que venga en el error no llegue a la pantalla.
 */
public class ChannelConnectionChecker implements ChannelConnectionCheckPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String graphBaseUrl;
    private final String graphVersion;
    private final String geminiBaseUrl;

    public ChannelConnectionChecker(RestClient restClient, ObjectMapper objectMapper, String graphBaseUrl,
                                    String graphVersion, String geminiBaseUrl) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.graphBaseUrl = graphBaseUrl;
        this.graphVersion = graphVersion;
        this.geminiBaseUrl = geminiBaseUrl;
    }

    @Override
    public CheckResult checkWhatsApp(String phoneNumberId, String accessToken) {
        try {
            String body = restClient.get()
                    .uri(graphBaseUrl + "/" + graphVersion + "/" + phoneNumberId + "?fields=verified_name,display_phone_number")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
            JsonNode number = objectMapper.readTree(body == null ? "{}" : body);
            String name = number.path("verified_name").asText("");
            String phone = number.path("display_phone_number").asText("");
            return new CheckResult(true, phone.isBlank() ? name : name + " (" + phone + ")");
        } catch (RestClientResponseException rejected) {
            return new CheckResult(false, "Meta rechazó las credenciales (HTTP " + rejected.getStatusCode().value()
                    + "). Revisa el identificador del número y el token de acceso.");
        } catch (RestClientException | JsonProcessingException unreachable) {
            return new CheckResult(false, "No se pudo conectar con Meta. Intenta de nuevo en unos minutos.");
        }
    }

    @Override
    public CheckResult checkGemini(String apiKey, String model) {
        try {
            String body = restClient.get()
                    .uri(geminiBaseUrl + "/v1beta/models/" + model)
                    .header("x-goog-api-key", apiKey)
                    .retrieve()
                    .body(String.class);
            String displayName = objectMapper.readTree(body == null ? "{}" : body).path("displayName").asText(model);
            return new CheckResult(true, displayName + " disponible");
        } catch (RestClientResponseException rejected) {
            return new CheckResult(false, "Gemini rechazó la clave o el modelo (HTTP " + rejected.getStatusCode().value() + ").");
        } catch (RestClientException | JsonProcessingException unreachable) {
            return new CheckResult(false, "No se pudo conectar con Gemini. Intenta de nuevo en unos minutos.");
        }
    }
}
