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
import com.jclinical.automation.domain.service.InboundWhatsAppProcessor;
import com.jclinical.automation.domain.service.ChatHistoryService;
import com.jclinical.automation.domain.service.RecordingOutboundQueue;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcChatAccessLog;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcChatHistory;
import com.jclinical.automation.domain.service.OutboundDispatcher;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcDeliveryStatusRecorder;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcOutboundDispatchRepository;
import com.jclinical.automation.infra.adapters.out.whatsapp.MetaWhatsAppSender;
import com.jclinical.automation.domain.service.WhatsAppWebhookService;
import com.jclinical.automation.infra.adapters.in.webhook.MetaWebhookPayloadParser;
import com.jclinical.automation.infra.adapters.in.webhook.WebhookRateLimiter;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcInboundMessageLedger;
import com.jclinical.automation.infra.adapters.out.connection.ChannelConnectionChecker;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcChannelSettingsAudit;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcChannelSettingsRepository;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.IntentInterpreterPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;
import com.jclinical.automation.infra.adapters.out.crossmodule.DoctorDirectoryAdapter;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcDoctorChannelRepository;
import com.jclinical.automation.domain.ports.out.DoctorChannelRepositoryPort;
import com.jclinical.automation.domain.service.DoctorChannelService;
import com.jclinical.automation.domain.ports.out.RealtimeNotifierPort;
import com.jclinical.automation.domain.service.NotifyingChatHistory;
import com.jclinical.automation.domain.service.RealtimeAccessPolicy;
import com.jclinical.automation.infra.adapters.out.realtime.SpringRealtimeNotifier;
import com.jclinical.automation.domain.service.DoctorNotificationService;
import com.jclinical.automation.infra.adapters.out.crossmodule.PatientDirectoryAdapter;
import com.jclinical.automation.infra.adapters.out.crossmodule.SlotAvailabilityAdapter;
import com.jclinical.automation.infra.adapters.out.gemini.GeminiIntentInterpreter;
import com.jclinical.automation.infra.adapters.out.persistence.ConversationOptionsCodec;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcConversationRepository;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Base64;

/**
 * Piezas de la automatizacion de citas: motor de conversacion, solicitudes al medico y sus
 * adaptadores. Los servicios de dominio se publican sin envolver; sus envoltorios transaccionales
 * ({@link TransactionalConversationUseCase}, {@link TransactionalAppointmentRequestUseCase}) son los
 * que usan los adaptadores de entrada.
 *
 * <p>Gemini usa la clave y el modelo que cada clinica guarda en su configuracion; no hay clave de
 * plataforma. GEMINI_BASE_URL solo cambia el servidor (pruebas).
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
    public JdbcAppointmentRequestRepository appointmentRequestRepository(JdbcTemplate jdbcTemplate, ProposedOptionsCodec codec) {
        return new JdbcAppointmentRequestRepository(jdbcTemplate, codec);
    }

    @Bean
    public OutboundMessageQueuePort outboundMessageQueue(JdbcTemplate jdbcTemplate, ConversationOptionsCodec codec,
                                                         ObjectMapper objectMapper, ChatHistoryPort chatHistory) {
        Clock clock = Clock.systemDefaultZone();
        return new RecordingOutboundQueue(new JdbcOutboundMessageQueue(jdbcTemplate, codec, objectMapper, clock),
                chatHistory, clock);
    }

    @Bean
    public JdbcInboundMessageLedger inboundMessageLedger(JdbcTemplate jdbcTemplate) {
        return new JdbcInboundMessageLedger(jdbcTemplate);
    }

    @Bean
    public OutboundDispatcher outboundDispatcher(
            JdbcTemplate jdbcTemplate, ConversationOptionsCodec codec, ObjectMapper objectMapper,
            ChannelSettingsRepositoryPort settings, JdbcInboundMessageLedger inboundMessageLedger,
            @Value("${app.automation.whatsapp.graph-base-url:https://graph.facebook.com}") String graphBaseUrl,
            @Value("${app.automation.whatsapp.graph-version:v23.0}") String graphVersion) {
        return new OutboundDispatcher(new JdbcOutboundDispatchRepository(jdbcTemplate, codec, objectMapper), settings,
                inboundMessageLedger, new MetaWhatsAppSender(externalRestClient(), objectMapper, graphBaseUrl, graphVersion),
                Clock.systemDefaultZone());
    }

    @Bean
    public SlotBookingPort automationSlotBooking(OnlineBookingService onlineBookingService) {
        return new SlotBookingAdapter(onlineBookingService);
    }

    @Bean
    public AppointmentRequestService automationAppointmentRequestService(
            AppointmentRequestRepositoryPort requests, SlotBookingPort booking, SlotAvailabilityPort slots,
            DomainEventPublisherPort events, DoctorNotificationService doctorNotifications) {
        return new AppointmentRequestService(requests, booking, slots, events, doctorNotifications, Clock.systemDefaultZone());
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

    /** Cifrado de secretos y del historial de chats, con la misma llave que el resto de Clinerya. */
    @Bean
    public FieldCipher automationFieldCipher(
            @Value("${medicloud.security.encryption-key}") String encryptionKey,
            @Value("${medicloud.security.key-store-dir:/app/secrets}") String keyStoreDir) {
        return FieldCipher.fromConfiguration(
                encryptionKey, keyStoreDir == null || keyStoreDir.isBlank() ? null : Path.of(keyStoreDir));
    }

    @Bean
    public ChannelSettingsRepositoryPort channelSettingsRepository(JdbcTemplate jdbcTemplate, FieldCipher automationFieldCipher) {
        return new JdbcChannelSettingsRepository(jdbcTemplate, automationFieldCipher);
    }

    @Bean
    public ChatHistoryPort chatHistory(JdbcTemplate jdbcTemplate, FieldCipher automationFieldCipher, ObjectMapper objectMapper,
                                       RealtimeNotifierPort realtimeNotifier) {
        return new NotifyingChatHistory(new JdbcChatHistory(jdbcTemplate, automationFieldCipher, objectMapper),
                realtimeNotifier);
    }

    // ---- Tiempo real (entrega 5.7) ---------------------------------------------------------------

    @Bean
    public RealtimeNotifierPort realtimeNotifier(ApplicationEventPublisher events) {
        return new SpringRealtimeNotifier(events);
    }

    @Bean
    public RealtimeAccessPolicy realtimeAccessPolicy(StaffPermissionCheckerPort permissions, SlotBookingPort booking) {
        return new RealtimeAccessPolicy(permissions, booking::doctorStaffIdOfUser);
    }

    @Bean
    public ChatHistoryService chatHistoryService(ChatHistoryPort chatHistory, JdbcTemplate jdbcTemplate,
                                                 PatientDirectoryPort automationPatientDirectory,
                                                 ChannelSettingsRepositoryPort settings,
                                                 StaffPermissionCheckerPort permissions) {
        return new ChatHistoryService(chatHistory, new JdbcChatAccessLog(jdbcTemplate), automationPatientDirectory,
                settings, permissions, Clock.systemDefaultZone());
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
    public WhatsAppWebhookService whatsAppWebhookService(ChannelSettingsRepositoryPort settings, ObjectMapper objectMapper,
                                                         JdbcInboundMessageLedger inboundMessageLedger,
                                                         JdbcTemplate jdbcTemplate, DomainEventPublisherPort events,
                                                         ChatHistoryPort chatHistory, DoctorChannelRepositoryPort doctorChannels) {
        return new WhatsAppWebhookService(settings, new MetaWebhookPayloadParser(objectMapper, ZoneId.systemDefault()),
                inboundMessageLedger, new JdbcDeliveryStatusRecorder(jdbcTemplate), chatHistory, doctorChannels, events,
                Clock.systemDefaultZone());
    }

    /** Usa el motor sin envolver: corre dentro de la transaccion de TransactionalInboundWhatsAppProcessor. */
    @Bean
    public InboundWhatsAppProcessor inboundWhatsAppProcessor(ConversationService automationConversationService,
                                                             OutboundMessageQueuePort outbound, ChatHistoryPort chatHistory,
                                                             DoctorNotificationService doctorNotifications) {
        return new InboundWhatsAppProcessor(automationConversationService, outbound, chatHistory, doctorNotifications);
    }

    // ---- Aviso al medico (entrega 5.6, D8) ------------------------------------------------------

    @Bean
    public DoctorChannelRepositoryPort doctorChannelRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcDoctorChannelRepository(jdbcTemplate);
    }

    @Bean
    public DoctorChannelService doctorChannelService(DoctorChannelRepositoryPort doctorChannels, SlotBookingPort booking,
                                                     DoctorDirectoryPort doctors, StaffPermissionCheckerPort permissions) {
        return new DoctorChannelService(doctorChannels, booking::doctorStaffIdOfUser, doctors, permissions,
                Clock.systemDefaultZone());
    }

    /**
     * Los avisos al medico van directo a la cola (no al historial de chats de pacientes). El enlace
     * lleva a la bandeja de solicitudes en Clinerya.
     */
    @Bean
    public DoctorNotificationService doctorNotificationService(
            DoctorChannelRepositoryPort doctorChannels, JdbcTemplate jdbcTemplate, ConversationOptionsCodec codec,
            ObjectMapper objectMapper, JdbcAppointmentRequestRepository requests, RealtimeNotifierPort realtimeNotifier,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${app.automation.doctor-inbox-path:/solicitudes-de-cita}") String inboxPath) {
        Clock clock = Clock.systemDefaultZone();
        return new DoctorNotificationService(doctorChannels,
                new JdbcOutboundMessageQueue(jdbcTemplate, codec, objectMapper, clock), requests, realtimeNotifier,
                frontendBaseUrl.replaceAll("/+$", "") + inboxPath, clock);
    }

    @Bean
    public WebhookRateLimiter whatsAppWebhookRateLimiter(
            @Value("${app.automation.whatsapp.webhook-max-requests-per-minute:600}") int maxRequestsPerMinute) {
        return new WebhookRateLimiter(maxRequestsPerMinute, System::currentTimeMillis);
    }

    /** Gemini con la clave y el modelo de cada clinica (D4); no hay clave de plataforma. */
    @Bean
    public IntentInterpreterPort geminiIntentInterpreter(
            ObjectMapper objectMapper, ChannelSettingsRepositoryPort channelSettings,
            @Value("${app.automation.gemini.base-url:${GEMINI_BASE_URL:https://generativelanguage.googleapis.com}}") String baseUrl) {
        return new GeminiIntentInterpreter(externalRestClient(), objectMapper, baseUrl, channelSettings);
    }
}
