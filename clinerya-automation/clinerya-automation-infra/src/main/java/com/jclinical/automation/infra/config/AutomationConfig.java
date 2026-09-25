package com.jclinical.automation.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.agenda.domain.service.OnlineBookingService;
import com.jclinical.automation.domain.ports.out.AppointmentRequestRepositoryPort;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;
import com.jclinical.automation.domain.ports.out.SlotBookingPort;
import com.jclinical.automation.domain.service.AppointmentRequestService;
import com.jclinical.automation.domain.service.ConversationService;
import com.jclinical.automation.infra.adapters.in.messaging.RequestOutcomeProcessor;
import com.jclinical.automation.infra.adapters.out.crossmodule.SlotBookingAdapter;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcAppointmentRequestRepository;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcOutboundMessageQueue;
import com.jclinical.automation.infra.adapters.out.persistence.ProposedOptionsCodec;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.IntentInterpreterPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;
import com.jclinical.automation.infra.adapters.out.crossmodule.DoctorDirectoryAdapter;
import com.jclinical.automation.infra.adapters.out.crossmodule.PatientDirectoryAdapter;
import com.jclinical.automation.infra.adapters.out.crossmodule.SlotAvailabilityAdapter;
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

import java.time.Clock;
import java.time.Duration;

/**
 * Piezas de la automatizacion de citas: motor de conversacion, solicitudes al medico y sus
 * adaptadores. Los servicios de dominio se publican sin envolver; sus envoltorios transaccionales
 * ({@link TransactionalConversationUseCase}, {@link TransactionalAppointmentRequestUseCase}) son los
 * que usan los adaptadores de entrada.
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
    public SlotAvailabilityPort automationSlotAvailability(OnlineBookingUseCase onlineBooking) {
        return new SlotAvailabilityAdapter(onlineBooking);
    }

    @Bean
    public ProposedOptionsCodec proposedOptionsCodec(ObjectMapper objectMapper) {
        return new ProposedOptionsCodec(objectMapper);
    }

    @Bean
    public AppointmentRequestRepositoryPort appointmentRequestRepository(JdbcTemplate jdbcTemplate, ProposedOptionsCodec codec) {
        return new JdbcAppointmentRequestRepository(jdbcTemplate, codec);
    }

    @Bean
    public OutboundMessageQueuePort outboundMessageQueue(JdbcTemplate jdbcTemplate, ConversationOptionsCodec codec) {
        return new JdbcOutboundMessageQueue(jdbcTemplate, codec, Clock.systemDefaultZone());
    }

    @Bean
    public SlotBookingPort automationSlotBooking(OnlineBookingService onlineBookingService) {
        return new SlotBookingAdapter(onlineBookingService);
    }

    @Bean
    public AppointmentRequestService automationAppointmentRequestService(
            AppointmentRequestRepositoryPort requests, SlotBookingPort booking, SlotAvailabilityPort slots,
            DomainEventPublisherPort events) {
        return new AppointmentRequestService(requests, booking, slots, events, Clock.systemDefaultZone());
    }

    @Bean
    public ConversationService automationConversationService(
            ConversationRepositoryPort conversations, PatientDirectoryPort patients, DoctorDirectoryPort doctors,
            SlotAvailabilityPort slots, AppointmentRequestService requests, IntentInterpreterPort interpreter) {
        return new ConversationService(conversations, patients, doctors, slots, requests, interpreter,
                Clock.systemDefaultZone());
    }

    @Bean
    public RequestOutcomeProcessor requestOutcomeProcessor(ConversationService conversations,
                                                           OutboundMessageQueuePort outbound) {
        return new RequestOutcomeProcessor(conversations, outbound);
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
