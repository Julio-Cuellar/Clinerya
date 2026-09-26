package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.model.DoctorChannel;
import com.jclinical.automation.domain.model.DeliveryStatusUpdate;
import com.jclinical.automation.domain.model.WebhookPayload;
import com.jclinical.automation.domain.model.WebhookSignature;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage.Kind;
import com.jclinical.automation.domain.model.WhatsAppMessageReceivedEvent;
import com.jclinical.automation.domain.ports.in.ReceiveWhatsAppWebhookUseCase.Receipt;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.service.ChannelSettingsServiceTest.InMemorySettings;
import com.jclinical.automation.domain.service.DoctorChannelServiceTest.InMemoryChannels;
import com.jclinical.core.events.DomainEventRoutingKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entrega 5.3: el webhook publico de cada clinica. Solo acepta lo que Meta firmo con el secreto de
 * esa clinica, descarta repetidos y nunca procesa la conversacion en la peticion: publica un evento
 * y responde de inmediato.
 */
class WhatsAppWebhookServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 10, 0);
    private static final String PHONE_NUMBER_ID = "106540352242922";
    private static final String APP_SECRET = "secreto-de-la-app";
    private static final byte[] BODY = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);

    private final UUID clinicId = UUID.randomUUID();
    private final InMemorySettings settings = new InMemorySettings();
    private final List<WhatsAppInboundMessage> parsed = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private final List<String> senders = new ArrayList<>();
    private final List<Object[]> published = new ArrayList<>();
    private final List<DeliveryStatusUpdate> statuses = new ArrayList<>();
    private final List<DeliveryStatusUpdate> recordedStatuses = new ArrayList<>();
    private final List<ChatMessage> history = new ArrayList<>();
    private final InMemoryChannels doctorChannels = new InMemoryChannels();

    private WhatsAppWebhookService service;

    @BeforeEach
    void setUp() {
        settings.save(ChannelSettings.unconfigured(clinicId).toBuilder()
                .whatsappPhoneNumberId(PHONE_NUMBER_ID).whatsappAppSecret(APP_SECRET)
                .webhookKey("llave-webhook").verifyToken("token-verificacion").enabled(true).build());
        service = new WhatsAppWebhookService(settings, body -> new WebhookPayload(parsed, statuses),
                (clinic, waMessageId, fromPhone, at) -> {
                    senders.add(fromPhone);
                    return seen.add(waMessageId);
                },
                (clinic, update) -> recordedStatuses.add(update),
                new RecordingHistory(history),
                doctorChannels,
                (routingKey, payload) -> published.add(new Object[]{routingKey, payload}),
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    // ---- verificacion de Meta ---------------------------------------------------------------

    @Test
    void metaVerificationEchoesTheChallengeOnlyWithTheRightToken() {
        assertEquals(Optional.of("1158201444"),
                service.verifySubscription("llave-webhook", "subscribe", "token-verificacion", "1158201444"));
        assertTrue(service.verifySubscription("llave-webhook", "subscribe", "otro-token", "1158201444").isEmpty());
        assertTrue(service.verifySubscription("llave-webhook", "unsubscribe", "token-verificacion", "1").isEmpty());
        assertTrue(service.verifySubscription("otra-llave", "subscribe", "token-verificacion", "1").isEmpty());
    }

    // ---- firma ------------------------------------------------------------------------------

    @Test
    void theSignatureIsTheHmacOfTheRawBodyWithTheAppSecret() {
        String header = WebhookSignature.sign(BODY, APP_SECRET);

        assertTrue(header.startsWith("sha256="));
        assertEquals(64, header.length() - "sha256=".length());
        assertTrue(WebhookSignature.isValid(BODY, header, APP_SECRET));
        assertFalse(WebhookSignature.isValid(BODY, header, "otro-secreto"));
        assertFalse(WebhookSignature.isValid("{}".getBytes(StandardCharsets.UTF_8), header, APP_SECRET));
        assertFalse(WebhookSignature.isValid(BODY, null, APP_SECRET));
        assertFalse(WebhookSignature.isValid(BODY, "sha1=abc", APP_SECRET));
    }

    @Test
    void anUnsignedOrForgedRequestIsRejectedWithoutReadingIt() {
        parsed.add(text("wamid.1", "Hola"));

        assertEquals(Receipt.INVALID_SIGNATURE, service.receive("llave-webhook", BODY, null));
        assertEquals(Receipt.INVALID_SIGNATURE, service.receive("llave-webhook", BODY, WebhookSignature.sign(BODY, "otro")));
        assertTrue(published.isEmpty());
        assertTrue(seen.isEmpty());
    }

    @Test
    void anUnknownWebhookIsRejected() {
        assertEquals(Receipt.UNKNOWN_WEBHOOK, service.receive("no-existe", BODY, WebhookSignature.sign(BODY, APP_SECRET)));
    }

    @Test
    void aClinicWithoutAppSecretCannotAcceptMessages() {
        settings.save(settings.stored.get(clinicId).toBuilder().whatsappAppSecret(null).build());

        assertEquals(Receipt.INVALID_SIGNATURE, service.receive("llave-webhook", BODY, WebhookSignature.sign(BODY, APP_SECRET)));
    }

    // ---- aceptacion -------------------------------------------------------------------------

    @Test
    void signedMessagesArePublishedForBackgroundProcessing() {
        parsed.add(text("wamid.1", "Hola"));
        parsed.add(new WhatsAppInboundMessage(PHONE_NUMBER_ID, "wamid.2", "5215512345678", Kind.OPTION, "Agendar",
                "action:book", NOW.minusSeconds(5)));

        Receipt receipt = service.receive("llave-webhook", BODY, WebhookSignature.sign(BODY, APP_SECRET));

        assertEquals(Receipt.ACCEPTED, receipt);
        assertEquals(2, published.size());
        assertEquals(DomainEventRoutingKeys.WHATSAPP_MESSAGE_RECEIVED, published.get(0)[0]);
        WhatsAppMessageReceivedEvent first = (WhatsAppMessageReceivedEvent) published.get(0)[1];
        assertEquals(clinicId, first.clinicId());
        assertEquals("wamid.1", first.waMessageId());
        assertEquals("5215512345678", first.fromPhone());
        assertEquals("Hola", first.text());
        assertEquals(NOW, first.receivedAt());
        WhatsAppMessageReceivedEvent second = (WhatsAppMessageReceivedEvent) published.get(1)[1];
        assertEquals("action:book", second.selectedOptionId());
        assertEquals(List.of("5215512345678", "5215512345678"), senders, "el remitente abre la ventana de 24 h");
    }

    @Test
    void aMessageMetaRetriesIsProcessedOnlyOnce() {
        parsed.add(text("wamid.1", "Hola"));
        String signature = WebhookSignature.sign(BODY, APP_SECRET);

        service.receive("llave-webhook", BODY, signature);
        service.receive("llave-webhook", BODY, signature);

        assertEquals(1, published.size());
        assertEquals(1, history.size(), "un reintento de Meta no duplica el mensaje en el historial");
    }

    @Test
    void aClinicWithTheAssistantOffKeepsTheMessageForTheStaffButDoesNotAnswer() {
        settings.save(settings.stored.get(clinicId).toBuilder().enabled(false).build());
        parsed.add(text("wamid.1", "Hola"));

        assertEquals(Receipt.IGNORED, service.receive("llave-webhook", BODY, WebhookSignature.sign(BODY, APP_SECRET)));

        assertTrue(published.isEmpty(), "el asistente apagado no contesta");
        assertEquals(1, history.size(), "pero el personal ve que el paciente escribio");
        assertEquals("Hola", history.get(0).text());
        assertEquals(ChatMessage.Direction.INBOUND, history.get(0).direction());
        assertEquals(List.of("5215512345678"), senders, "queda registrado (repetidos y ventana de 24 h)");
    }

    @Test
    void everyMessageIsKeptInTheHistoryBeforeTheAssistantRuns() {
        parsed.add(text("wamid.1", "Hola"));

        service.receive("llave-webhook", BODY, WebhookSignature.sign(BODY, APP_SECRET));

        assertEquals(1, published.size());
        assertEquals(1, history.size(), "si el asistente o Meta fallan despues, el mensaje ya esta guardado");
        ChatMessage kept = history.get(0);
        assertEquals(clinicId, kept.clinicId());
        assertEquals("5215512345678", kept.phone());
        assertEquals("Hola", kept.text());
        assertEquals(NOW, kept.at());
    }

    @Test
    void aMessageThatIsNotTextIsKeptAsAPlaceholder() {
        parsed.add(new WhatsAppInboundMessage(PHONE_NUMBER_ID, "wamid.3", "5215512345678", Kind.UNSUPPORTED, null, null, NOW));

        service.receive("llave-webhook", BODY, WebhookSignature.sign(BODY, APP_SECRET));

        assertEquals("[Mensaje que no es texto]", history.get(0).text());
    }

    @Test
    void aDoctorsMessageStaysOutOfThePatientsHistory() {
        doctorChannels.save(new DoctorChannel(clinicId, UUID.randomUUID(), "5215599990000", true, NOW, null, NOW));
        parsed.add(new WhatsAppInboundMessage(PHONE_NUMBER_ID, "wamid.4", "5215599990000", Kind.TEXT, "Acepto", null, NOW));

        service.receive("llave-webhook", BODY, WebhookSignature.sign(BODY, APP_SECRET));

        assertTrue(history.isEmpty());
        assertEquals(1, published.size(), "el procesador le contesta que responda en Clinerya");
    }

    @Test
    void messagesForAnotherNumberOfTheSameAppAreSkipped() {
        parsed.add(new WhatsAppInboundMessage("999999999999", "wamid.9", "5215512345678", Kind.TEXT, "Hola", null, NOW));

        assertEquals(Receipt.ACCEPTED, service.receive("llave-webhook", BODY, WebhookSignature.sign(BODY, APP_SECRET)));
        assertTrue(published.isEmpty());
    }

    @Test
    void metaStatusesAboutOurMessagesAreRecorded() {
        DeliveryStatusUpdate read = new DeliveryStatusUpdate(PHONE_NUMBER_ID, "wamid.OUT1",
                DeliveryStatusUpdate.Status.READ, NOW, null);
        statuses.add(read);
        statuses.add(new DeliveryStatusUpdate("999999999999", "wamid.OTRO", DeliveryStatusUpdate.Status.DELIVERED, NOW, null));

        assertEquals(Receipt.ACCEPTED, service.receive("llave-webhook", BODY, WebhookSignature.sign(BODY, APP_SECRET)));

        assertEquals(List.of(read), recordedStatuses, "solo los avisos del numero de la clinica");
    }

    @Test
    void statusesOfAClinicWithTheAssistantOffAreNotRecorded() {
        settings.save(settings.stored.get(clinicId).toBuilder().enabled(false).build());
        statuses.add(new DeliveryStatusUpdate(PHONE_NUMBER_ID, "wamid.OUT1", DeliveryStatusUpdate.Status.READ, NOW, null));

        service.receive("llave-webhook", BODY, WebhookSignature.sign(BODY, APP_SECRET));

        assertTrue(recordedStatuses.isEmpty());
    }

    /** Historial en memoria: solo interesa lo que se guarda. */
    static final class RecordingHistory implements ChatHistoryPort {
        private final List<ChatMessage> recorded;

        RecordingHistory(List<ChatMessage> recorded) {
            this.recorded = recorded;
        }

        @Override
        public void record(ChatMessage message) {
            recorded.add(message);
        }

        @Override
        public List<ChatSummary> findChats(UUID clinicId, int limit) {
            return List.of();
        }

        @Override
        public List<ChatMessage> findMessages(UUID clinicId, String phone, LocalDateTime before, int limit) {
            return List.of();
        }

        @Override
        public int deleteOlderThan(UUID clinicId, LocalDateTime cutoff) {
            return 0;
        }
    }

    private static WhatsAppInboundMessage text(String id, String body) {
        return new WhatsAppInboundMessage(PHONE_NUMBER_ID, id, "5215512345678", Kind.TEXT, body, null, NOW.minusSeconds(3));
    }
}
