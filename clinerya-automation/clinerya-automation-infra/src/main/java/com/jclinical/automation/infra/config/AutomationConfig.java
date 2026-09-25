package com.jclinical.automation.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.IntentInterpreterPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort;
import com.jclinical.automation.infra.adapters.out.crossmodule.DoctorDirectoryAdapter;
import com.jclinical.automation.infra.adapters.out.crossmodule.PatientDirectoryAdapter;
import com.jclinical.automation.infra.adapters.out.gemini.GeminiIntentInterpreter;
import com.jclinical.automation.infra.adapters.out.persistence.ConversationOptionsCodec;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcConversationRepository;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Piezas de infraestructura de la automatizacion de citas. El motor (ConversationService) se
 * publica como bean cuando existan los adaptadores de cupos y solicitudes (entregas 3 y 4).
 *
 * <p>Gemini se configura por variables de entorno: GEMINI_API_KEY (sin ella el interprete nunca
 * llama y la conversacion funciona solo con botones), GEMINI_MODEL y GEMINI_BASE_URL.
 */
@Configuration
public class AutomationConfig {

    @Bean
    public ConversationOptionsCodec conversationOptionsCodec(ObjectMapper objectMapper) {
        return new ConversationOptionsCodec(objectMapper);
    }

    @Bean
    public ConversationRepositoryPort conversationRepository(JdbcTemplate jdbcTemplate, ConversationOptionsCodec codec) {
        return new JdbcConversationRepository(jdbcTemplate, codec);
    }

    @Bean
    public PatientDirectoryPort automationPatientDirectory(GetPatientUseCase getPatientUseCase) {
        return new PatientDirectoryAdapter(getPatientUseCase);
    }

    @Bean
    public DoctorDirectoryPort automationDoctorDirectory(ManageAppointmentsUseCase appointments) {
        return new DoctorDirectoryAdapter(appointments);
    }

    @Bean
    public IntentInterpreterPort geminiIntentInterpreter(
            ObjectMapper objectMapper,
            @Value("${app.automation.gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${app.automation.gemini.model:${GEMINI_MODEL:gemini-2.5-flash}}") String model,
            @Value("${app.automation.gemini.base-url:${GEMINI_BASE_URL:https://generativelanguage.googleapis.com}}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
        return new GeminiIntentInterpreter(restClient, objectMapper, baseUrl, model, apiKey);
    }
}
