package com.jclinical.automation.infra.config;

import com.jclinical.agenda.domain.ports.in.ManageClinicScheduleUseCase;
import com.jclinical.automation.domain.ports.out.ClinicInfoPort;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort;
import com.jclinical.automation.domain.agent.AgentConversationService;
import com.jclinical.automation.domain.agent.AgentPersonas;
import com.jclinical.automation.domain.agent.AgentRequestOutcomeService;
import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ConversationAgent;
import com.jclinical.automation.domain.agent.tools.AcceptConsentTool;
import com.jclinical.automation.domain.agent.tools.ChooseProposalTool;
import com.jclinical.automation.domain.agent.tools.ClinicInfoTool;
import com.jclinical.automation.domain.agent.tools.ConfirmActionTool;
import com.jclinical.automation.domain.agent.tools.ConsentTool;
import com.jclinical.automation.domain.agent.tools.DoctorsTool;
import com.jclinical.automation.domain.agent.tools.MyAppointmentsTool;
import com.jclinical.automation.domain.agent.tools.ProposeBookingTool;
import com.jclinical.automation.domain.agent.tools.ProposeCancellationTool;
import com.jclinical.automation.domain.agent.tools.ProposeRescheduleTool;
import com.jclinical.automation.domain.agent.tools.RegisterPatientTool;
import com.jclinical.automation.domain.agent.tools.ServicesTool;
import com.jclinical.automation.domain.agent.tools.SlotsTool;
import com.jclinical.automation.domain.ports.out.AppointmentCancellationPort;
import com.jclinical.automation.domain.ports.out.AssistantProfileStorePort;
import com.jclinical.automation.domain.service.AssistantProfileService;
import com.jclinical.automation.domain.ports.out.AssistantProfilePort;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort;
import com.jclinical.automation.infra.adapters.out.crossmodule.AppointmentCancellationAdapter;
import com.jclinical.automation.infra.adapters.out.crossmodule.PatientAppointmentsAdapter;
import com.jclinical.automation.infra.adapters.out.crossmodule.TreatmentCatalogAdapter;
import com.jclinical.automation.infra.adapters.out.gemini.GeminiConversationModel;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcAssistantProfileRepository;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcPendingActionRepository;
import com.jclinical.treatments.domain.ports.in.PublicTreatmentCatalogUseCase;
import com.jclinical.automation.infra.adapters.out.crossmodule.ClinicInfoAdapter;
import com.jclinical.automation.infra.adapters.out.crossmodule.PatientRegistrationAdapter;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcRegistrationDraftRepository;
import com.jclinical.clinics.domain.ports.in.GetClinicPublicProfileUseCase;
import com.jclinical.patients.domain.ports.in.RecordContactConsentUseCase;
import com.jclinical.patients.domain.ports.in.RegisterPatientUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.agenda.domain.service.OnlineBookingService;
import com.jclinical.automation.domain.ports.out.AppointmentRequestRepositoryPort;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;
import com.jclinical.automation.domain.ports.out.SlotBookingPort;
import com.jclinical.automation.domain.service.AppointmentRequestService;
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
import com.jclinical.automation.domain.service.ChatAttentionService;
import com.jclinical.automation.domain.service.ChatContactService;
import com.jclinical.automation.domain.ports.out.ChatContactsPort;
import com.jclinical.automation.infra.adapters.out.crossmodule.PatientSnapshotAdapter;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcChatContacts;
import com.jclinical.automation.domain.ports.out.ChatAttentionLogPort;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcChatAttentionLog;
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
import java.util.List;

/**
 * Piezas de la automatizacion de citas: el agente conversacional y sus herramientas, las solicitudes
 * al medico y sus adaptadores. Los servicios de dominio se publican sin envolver; sus envoltorios
 * transaccionales ({@link TransactionalConversationUseCase}, {@link TransactionalAppointmentRequestUseCase})
 * son los que usan los adaptadores de entrada.
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

    /** Reprogramar: la cita original se cancela por la ruta de la agenda solo cuando el medico aprueba la nueva. */
    @Bean
    public AppointmentRequestService automationAppointmentRequestService(
            AppointmentRequestRepositoryPort requests, SlotBookingPort booking, SlotAvailabilityPort slots,
            DomainEventPublisherPort events, DoctorNotificationService doctorNotifications,
            AppointmentCancellationPort automationAppointmentCancellation) {
        AppointmentRequestService service = new AppointmentRequestService(requests, booking, slots, events,
                doctorNotifications, Clock.systemDefaultZone());
        service.setAppointmentCancellation(automationAppointmentCancellation);
        return service;
    }

    // ---- Agente conversacional (plan agente-conversacional) -------------------------------------

    @Bean
    public AppointmentCancellationPort automationAppointmentCancellation(OnlineBookingUseCase onlineBooking) {
        return new AppointmentCancellationAdapter(onlineBooking);
    }

    @Bean
    public JdbcPendingActionRepository agentPendingActions(JdbcTemplate jdbcTemplate, FieldCipher automationFieldCipher) {
        return new JdbcPendingActionRepository(jdbcTemplate, automationFieldCipher);
    }

    @Bean
    public AssistantProfileStorePort assistantProfiles(JdbcTemplate jdbcTemplate) {
        return new JdbcAssistantProfileRepository(jdbcTemplate);
    }

    @Bean
    public AssistantProfileService assistantProfileService(AssistantProfileStorePort assistantProfiles,
                                                           ChannelSettingsRepositoryPort channelSettings,
                                                           StaffPermissionCheckerPort permissions,
                                                           ChannelSettingsAuditPort audit) {
        return new AssistantProfileService(assistantProfiles, channelSettings, permissions, audit, Clock.systemDefaultZone());
    }

    /**
     * Las herramientas solo usan rutas publicas de los otros modulos (agenda, pacientes, clinicas,
     * tratamientos); el agente nunca consulta sus tablas.
     */
    @Bean
    public ConversationAgent conversationAgent(
            ObjectMapper objectMapper, ChannelSettingsRepositoryPort channelSettings,
            @Value("${app.automation.gemini.base-url:${GEMINI_BASE_URL:https://generativelanguage.googleapis.com}}") String baseUrl,
            ConversationRepositoryPort conversations, DoctorDirectoryPort doctors, SlotAvailabilityPort slots,
            ClinicInfoPort automationClinicInfo, AssistantProfilePort assistantProfiles,
            PublicTreatmentCatalogUseCase treatmentCatalog, OnlineBookingUseCase onlineBooking,
            JdbcPendingActionRepository agentPendingActions, AppointmentRequestService requests,
            AppointmentCancellationPort automationAppointmentCancellation, DoctorNotificationService doctorNotifications,
            PatientRegistrationPort automationPatientRegistration) {
        PatientAppointmentsPort appointments = new PatientAppointmentsAdapter(onlineBooking);
        TreatmentCatalogAdapter serviceCatalog = new TreatmentCatalogAdapter(treatmentCatalog);
        List<AgentTool> tools = List.of(
                new ClinicInfoTool(automationClinicInfo, assistantProfiles),
                new ServicesTool(serviceCatalog, assistantProfiles),
                new DoctorsTool(doctors),
                new SlotsTool(doctors, slots, serviceCatalog),
                new MyAppointmentsTool(appointments, doctors),
                new ProposeBookingTool(doctors, agentPendingActions),
                new ProposeCancellationTool(appointments, doctors, agentPendingActions),
                new ProposeRescheduleTool(appointments, doctors, agentPendingActions),
                new ConfirmActionTool(agentPendingActions, requests, conversations, automationAppointmentCancellation,
                        doctorNotifications),
                new ChooseProposalTool(requests, conversations),
                new ConsentTool(automationPatientRegistration, automationClinicInfo, agentPendingActions),
                new AcceptConsentTool(agentPendingActions),
                new RegisterPatientTool(automationPatientRegistration, agentPendingActions, doctors));
        return new ConversationAgent(
                new GeminiConversationModel(agentRestClient(), objectMapper, baseUrl, channelSettings), tools);
    }

    @Bean
    public AgentConversationService agentConversationService(
            ConversationRepositoryPort conversations, ChatHistoryPort chatHistory, PatientDirectoryPort patients,
            ClinicInfoPort automationClinicInfo, AssistantProfilePort assistantProfiles,
            ChannelSettingsRepositoryPort channelSettings, ConversationAgent conversationAgent,
            ChatAttentionLogPort chatAttentionLog) {
        return new AgentConversationService(conversations, chatHistory, patients,
                new AgentPersonas(automationClinicInfo, assistantProfiles, channelSettings::findByClinicId),
                conversationAgent, Clock.systemDefaultZone(), chatAttentionLog);
    }

    @Bean
    public AgentRequestOutcomeService agentRequestOutcomeService(ConversationRepositoryPort conversations) {
        return new AgentRequestOutcomeService(conversations);
    }

    // ---- Numeros que aun no son pacientes (CU-4) -----------------------------------------------

    @Bean
    public ClinicInfoPort automationClinicInfo(GetClinicPublicProfileUseCase profiles, ManageClinicScheduleUseCase schedules) {
        return new ClinicInfoAdapter(profiles, schedules);
    }

    @Bean
    public PatientRegistrationPort automationPatientRegistration(RegisterPatientUseCase registerPatient,
                                                                 RecordContactConsentUseCase contactConsent) {
        return new PatientRegistrationAdapter(registerPatient, contactConsent);
    }

    @Bean
    public JdbcRegistrationDraftRepository registrationDrafts(JdbcTemplate jdbcTemplate, FieldCipher automationFieldCipher) {
        return new JdbcRegistrationDraftRepository(jdbcTemplate, automationFieldCipher);
    }

    @Bean
    public RequestOutcomeProcessor requestOutcomeProcessor(AgentRequestOutcomeService conversations,
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
                                                 StaffPermissionCheckerPort permissions,
                                                 ChatAttentionLogPort chatAttentionLog, ChatContactsPort chatContacts) {
        return new ChatHistoryService(chatHistory, new JdbcChatAccessLog(jdbcTemplate), automationPatientDirectory,
                settings, permissions, Clock.systemDefaultZone(), chatAttentionLog, chatContacts);
    }

    // ---- Ficha del contacto en Chats -------------------------------------------------------------

    @Bean
    public ChatContactsPort chatContacts(JdbcTemplate jdbcTemplate, FieldCipher automationFieldCipher) {
        return new JdbcChatContacts(jdbcTemplate, automationFieldCipher);
    }

    /** Pacientes y citas por las rutas publicas de pacientes y agenda. */
    @Bean
    public ChatContactService chatContactService(ChatContactsPort chatContacts, GetPatientUseCase getPatientUseCase,
                                                 OnlineBookingUseCase onlineBooking, DoctorDirectoryPort doctors,
                                                 StaffPermissionCheckerPort permissions) {
        return new ChatContactService(chatContacts, new PatientSnapshotAdapter(getPatientUseCase, onlineBooking), doctors,
                permissions, Clock.systemDefaultZone());
    }

    // ---- Atencion humana en Chats (fase G) --------------------------------------------------------

    @Bean
    public ChatAttentionLogPort chatAttentionLog(JdbcTemplate jdbcTemplate) {
        return new JdbcChatAttentionLog(jdbcTemplate);
    }

    /** Los mensajes del personal pasan por la misma cola que registra todo lo que sale al paciente. */
    @Bean
    public ChatAttentionService chatAttentionService(ConversationRepositoryPort conversations, ChatHistoryPort chatHistory,
                                                     OutboundMessageQueuePort outbound, ChatAttentionLogPort chatAttentionLog,
                                                     RealtimeNotifierPort realtimeNotifier,
                                                     StaffPermissionCheckerPort permissions) {
        return new ChatAttentionService(conversations, chatHistory, outbound, chatAttentionLog, realtimeNotifier, permissions,
                Clock.systemDefaultZone());
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
        return restClient(Duration.ofSeconds(8));
    }

    /** El agente espera un poco mas: con herramientas, Gemini tarda mas en proponer el siguiente paso. */
    private static RestClient agentRestClient() {
        return restClient(Duration.ofSeconds(20));
    }

    private static RestClient restClient(Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Bean
    public WhatsAppWebhookService whatsAppWebhookService(ChannelSettingsRepositoryPort settings, ObjectMapper objectMapper,
                                                         JdbcInboundMessageLedger inboundMessageLedger,
                                                         JdbcTemplate jdbcTemplate, DomainEventPublisherPort events,
                                                         ChatHistoryPort chatHistory, DoctorChannelRepositoryPort doctorChannels,
                                                         ChatContactsPort chatContacts) {
        WhatsAppWebhookService service = new WhatsAppWebhookService(settings,
                new MetaWebhookPayloadParser(objectMapper, ZoneId.systemDefault()), inboundMessageLedger,
                new JdbcDeliveryStatusRecorder(jdbcTemplate), chatHistory, doctorChannels, events, Clock.systemDefaultZone());
        service.setChatContacts(chatContacts);
        return service;
    }

    /** Usa el agente sin envolver: corre dentro de la transaccion de TransactionalInboundWhatsAppProcessor. */
    @Bean
    public InboundWhatsAppProcessor inboundWhatsAppProcessor(AgentConversationService agentConversationService,
                                                             OutboundMessageQueuePort outbound,
                                                             DoctorNotificationService doctorNotifications) {
        return new InboundWhatsAppProcessor(agentConversationService, outbound, doctorNotifications);
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
}
