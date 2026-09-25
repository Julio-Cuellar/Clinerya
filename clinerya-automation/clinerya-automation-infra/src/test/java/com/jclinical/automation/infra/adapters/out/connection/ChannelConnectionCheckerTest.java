package com.jclinical.automation.infra.adapters.out.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.ports.out.ChannelConnectionCheckPort.CheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

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
 * "Probar conexion" consulta el numero en Meta y el modelo en Gemini con las credenciales de la
 * clinica. Nunca escribe nada y el detalle que vuelve a la pantalla jamas incluye el secreto.
 */
class ChannelConnectionCheckerTest {

    private static final String TOKEN = "EAAGm0PX4ZCpsBAKZCZBtoken1234567890";
    private static final String GEMINI_KEY = "AIzaSyFakeGeminiKey4321";
    private static final String PHONE_URL =
            "https://graph.facebook.com/v23.0/106540352242922?fields=verified_name,display_phone_number";
    private static final String MODEL_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash";

    private MockRestServiceServer server;
    private ChannelConnectionChecker checker;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        checker = new ChannelConnectionChecker(builder.build(), new ObjectMapper(),
                "https://graph.facebook.com", "v23.0", "https://generativelanguage.googleapis.com");
    }

    @Test
    void aWorkingWhatsAppNumberReportsItsVerifiedName() {
        server.expect(requestTo(PHONE_URL)).andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess("""
                        {"verified_name":"Clínica Sonrisa","display_phone_number":"+52 55 1234 5678","id":"106540352242922"}
                        """, MediaType.APPLICATION_JSON));

        CheckResult result = checker.checkWhatsApp("106540352242922", TOKEN);

        assertTrue(result.ok());
        assertEquals("Clínica Sonrisa (+52 55 1234 5678)", result.detail());
        server.verify();
    }

    @Test
    void aRejectedWhatsAppTokenIsReportedWithoutLeakingIt() {
        server.expect(requestTo(PHONE_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"message\":\"Invalid OAuth access token " + TOKEN + "\"}}"));

        CheckResult result = checker.checkWhatsApp("106540352242922", TOKEN);

        assertFalse(result.ok());
        assertTrue(result.detail().contains("401"), result.detail());
        assertFalse(result.detail().contains(TOKEN));
    }

    @Test
    void anUnreachableMetaIsReportedAsAFailure() {
        server.expect(requestTo(PHONE_URL)).andRespond(withException(new IOException("sin red")));

        CheckResult result = checker.checkWhatsApp("106540352242922", TOKEN);

        assertFalse(result.ok());
        assertTrue(result.detail().contains("Meta"), result.detail());
    }

    @Test
    void aWorkingGeminiKeyReportsTheModel() {
        server.expect(requestTo(MODEL_URL)).andExpect(method(HttpMethod.GET))
                .andExpect(header("x-goog-api-key", GEMINI_KEY))
                .andRespond(withSuccess("""
                        {"name":"models/gemini-2.5-flash","displayName":"Gemini 2.5 Flash"}
                        """, MediaType.APPLICATION_JSON));

        CheckResult result = checker.checkGemini(GEMINI_KEY, "gemini-2.5-flash");

        assertTrue(result.ok());
        assertEquals("Gemini 2.5 Flash disponible", result.detail());
        server.verify();
    }

    @Test
    void aRejectedGeminiKeyIsReportedWithoutLeakingIt() {
        server.expect(requestTo(MODEL_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"message\":\"API key not valid: " + GEMINI_KEY + "\"}}"));

        CheckResult result = checker.checkGemini(GEMINI_KEY, "gemini-2.5-flash");

        assertFalse(result.ok());
        assertTrue(result.detail().contains("400"), result.detail());
        assertFalse(result.detail().contains(GEMINI_KEY));
    }
}
