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
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.core.security.crypto.FieldCipher;
import com.jclinical.automation.domain.ports.out.ChannelConnectionCheckPort;
import com.jclinical.automation.domain.ports.out.ChannelSettingsAuditPort;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.automation.domain.service.ChannelSettingsService;
import com.jclinical.automation.infra.adapters.out.connection.ChannelConnectionChecker;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcChannelSettingsAudit;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcChannelSettingsRepository;
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

import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

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

    /** 32 bytes aleatorios: llave del webhook y token de verificacion. */
    private static final int RANDOM_TOKEN_BYTES = 32;

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
    public ChannelSettingsRepositoryPort channelSettingsRepository(
            JdbcTemplate jdbcTemplate,
            @Value("${medicloud.security.encryption-key}") String encryptionKey,
            @Value("${medicloud.security.key-store-dir:/app/secrets}") String keyStoreDir) {
        FieldCipher cipher = FieldCipher.fromConfiguration(
                encryptionKey, keyStoreDir == null || keyStoreDir.isBlank() ? null : Path.of(keyStoreDir));
        return new JdbcChannelSettingsRepository(jdbcTemplate, cipher);
    }

    @Bean
    public ChannelSettingsAuditPort channelSettingsAudit(JdbcTemplate jdbcTemplate) {
        return new JdbcChannelSettingsAudit(jdbcTemplate);
    }

    @Bean
    public ChannelConnectionCheckPort channelConnectionChecker(
            ObjectMapper objectMapper,
            @Value("${app.automation.whatsapp.graph-base-url:https://graph.facebook.com}") String graphBaseUrl,
            @Value("${app.automation.whatsapp.graph-version:v23.0}") String graphVersion,
            @Value("${app.automation.gemini.base-url:${GEMINI_BASE_URL:https://generativelanguage.googleapis.com}}") String geminiBaseUrl) {
        return new ChannelConnectionChecker(externalRestClient(), objectMapper, graphBaseUrl, graphVersion, geminiBaseUrl);
    }

    @Bean
    public ChannelSettingsService channelSettingsService(ChannelSettingsRepositoryPort settings,
                                                         ChannelSettingsAuditPort audit,
                                                         ChannelConnectionCheckPort checks,
                                                         StaffPermissionCheckerPort permissions) {
        SecureRandom random = new SecureRandom();
        return new ChannelSettingsService(settings, audit, checks, permissions, () -> {
            byte[] bytes = new byte[RANDOM_TOKEN_BYTES];
            random.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }, Clock.systemDefaultZone());
    }

    /** Cliente para Meta y Gemini: tiempos cortos para no dejar hilos colgados si el proveedor no responde. */
    private static RestClient externalRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().requestFactory(requestFactory).build();
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
