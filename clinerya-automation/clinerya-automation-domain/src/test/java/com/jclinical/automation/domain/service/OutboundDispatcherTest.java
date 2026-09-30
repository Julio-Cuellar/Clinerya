package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.QueuedMessage;
import com.jclinical.automation.domain.model.QueuedMessage.Audience;
import com.jclinical.automation.domain.ports.out.OutboundDispatchRepositoryPort;
import com.jclinical.automation.domain.ports.out.WhatsAppSenderPort;
import com.jclinical.automation.domain.service.ChannelSettingsServiceTest.InMemorySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entrega 5.4: la cola de salida se envia por la API de Meta con las credenciales de cada clinica.
 * Dentro de la ventana de 24 h va texto libre (se siente real); fuera de ella, WhatsApp solo acepta
 * una plantilla aprobada (D7). Fallas temporales se reintentan con espera creciente.
 */
class OutboundDispatcherTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 12, 0);
    private static final String PHONE = "5215512345678";

    private final UUID clinicId = UUID.randomUUID();
    private final InMemorySettings settings = new InMemorySettings();
    private final FakeQueue queue = new FakeQueue();
    private final FakeSender sender = new FakeSender();
    private LocalDateTime lastInbound = NOW.minusMinutes(5);

    private OutboundDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        settings.save(ChannelSettings.unconfigured(clinicId).toBuilder()
                .whatsappPhoneNumberId("106540352242922").whatsappAccessToken("token-clinica")
                .patientTemplateName("aviso_paciente").doctorTemplateName("aviso_medico").enabled(true).build());
        dispatcher = new OutboundDispatcher(queue, settings, (clinic, phone) -> Optional.ofNullable(lastInbound), sender,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test
    void withNothingDueItReportsThereWasNothingToSend() {
        assertFalse(dispatcher.dispatchNext());
    }

    @Test
    void insideTheWindowTheReplyGoesAsFreeTextWithItsOptions() {
        OutboundReply reply = new OutboundReply("¿Qué deseas hacer?", List.of(new ConversationOption("action:book", "Agendar una cita")));
        queue.due.add(message(Audience.PATIENT, reply, 0));

        assertTrue(dispatcher.dispatchNext());

        assertEquals(List.of("mensaje:" + PHONE + ":¿Qué deseas hacer?"), sender.calls);
        assertEquals("106540352242922", sender.lastCredentials.phoneNumberId());
        assertEquals("token-clinica", sender.lastCredentials.accessToken());
        assertEquals(List.of("sent:wamid.OUT:false"), queue.outcomes);
    }

    @Test
    void outsideTheWindowAPatientGetsTheApprovedTemplate() {
        lastInbound = NOW.minusHours(25);
        queue.due.add(message(Audience.PATIENT, OutboundReply.text("Tu cita quedó confirmada."), 0));

        dispatcher.dispatchNext();

        assertEquals(List.of("plantilla:" + PHONE + ":aviso_paciente:es_MX:[Tu cita quedó confirmada.]"), sender.calls);
        assertEquals(List.of("sent:wamid.OUT:true"), queue.outcomes);
    }

    @Test
    void aPhoneThatNeverWroteIsOutsideTheWindow() {
        lastInbound = null;
        queue.due.add(message(Audience.DOCTOR, OutboundReply.text("Nueva solicitud"), 0));

        dispatcher.dispatchNext();

        assertTrue(sender.calls.get(0).startsWith("plantilla:" + PHONE + ":aviso_medico"), sender.calls.toString());
    }

    @Test
    void withoutATemplateAMessageOutsideTheWindowFailsClearly() {
        lastInbound = NOW.minusDays(2);
        settings.save(settings.stored.get(clinicId).toBuilder().patientTemplateName(null).build());
        queue.due.add(message(Audience.PATIENT, OutboundReply.text("Hola"), 0));

        dispatcher.dispatchNext();

        assertTrue(sender.calls.isEmpty());
        assertTrue(queue.outcomes.get(0).startsWith("failed:1:"), queue.outcomes.toString());
        assertTrue(queue.outcomes.get(0).contains("plantilla"), queue.outcomes.toString());
    }

    @Test
    void aTemporaryFailureIsRetriedLaterWithGrowingWaits() {
        sender.next = WhatsAppSenderPort.SendResult.failed(true, "HTTP 503");
        queue.due.add(message(Audience.PATIENT, OutboundReply.text("Hola"), 0));
        queue.due.add(message(Audience.PATIENT, OutboundReply.text("Hola"), 2));

        dispatcher.dispatchNext();
        dispatcher.dispatchNext();

        assertEquals(List.of("retry:1:" + NOW.plusMinutes(1) + ":HTTP 503", "retry:3:" + NOW.plusMinutes(15) + ":HTTP 503"),
                queue.outcomes);
    }

    @Test
    void afterTheLastAttemptTheMessageFails() {
        sender.next = WhatsAppSenderPort.SendResult.failed(true, "HTTP 503");
        queue.due.add(message(Audience.PATIENT, OutboundReply.text("Hola"), OutboundDispatcher.MAX_ATTEMPTS - 1));

        dispatcher.dispatchNext();

        assertEquals(List.of("failed:" + OutboundDispatcher.MAX_ATTEMPTS + ":HTTP 503"), queue.outcomes);
    }

    @Test
    void aPermanentRejectionFailsAtOnce() {
        sender.next = WhatsAppSenderPort.SendResult.failed(false, "HTTP 400: número inválido");
        queue.due.add(message(Audience.PATIENT, OutboundReply.text("Hola"), 0));

        dispatcher.dispatchNext();

        assertEquals(List.of("failed:1:HTTP 400: número inválido"), queue.outcomes);
    }

    @Test
    void aReminderUsesItsOwnTemplateWithItsButtonsOutsideTheWindow() {
        lastInbound = NOW.minusHours(25);
        OutboundReply reply = new OutboundReply("Hola Ana, te recordamos tu cita", List.of(
                new ConversationOption("recordatorio:confirmar:1", "Confirmo"),
                new ConversationOption("recordatorio:cancelar:1", "Cancelar")));
        queue.due.add(new QueuedMessage(UUID.randomUUID(), clinicId, PHONE, Audience.PATIENT, reply, List.of("Ana", "Clínica"), 0,
                NOW.minusMinutes(1), "recordatorio_cita"));

        dispatcher.dispatchNext();

        assertEquals(List.of("plantilla:" + PHONE + ":recordatorio_cita:es_MX:[Ana, Clínica]:botones"
                + "[recordatorio:confirmar:1, recordatorio:cancelar:1]"), sender.calls);
        assertEquals(List.of("sent:wamid.OUT:true"), queue.outcomes);
    }

    @Test
    void aClinicWithTheAssistantOffSendsNothing() {
        settings.save(settings.stored.get(clinicId).toBuilder().enabled(false).build());
        queue.due.add(message(Audience.PATIENT, OutboundReply.text("Hola"), 0));

        dispatcher.dispatchNext();

        assertTrue(sender.calls.isEmpty());
        assertTrue(queue.outcomes.get(0).startsWith("failed:"), queue.outcomes.toString());
    }

    private QueuedMessage message(Audience audience, OutboundReply reply, int attempts) {
        return new QueuedMessage(UUID.randomUUID(), clinicId, PHONE, audience, reply, List.of(reply.text()), attempts,
                NOW.minusMinutes(1));
    }

    static final class FakeQueue implements OutboundDispatchRepositoryPort {
        final Deque<QueuedMessage> due = new ArrayDeque<>();
        final List<String> outcomes = new ArrayList<>();

        @Override
        public Optional<QueuedMessage> lockNextDue(LocalDateTime now) {
            return Optional.ofNullable(due.poll());
        }

        @Override
        public void markSent(UUID id, String waMessageId, boolean viaTemplate, LocalDateTime at) {
            outcomes.add("sent:" + waMessageId + ":" + viaTemplate);
        }

        @Override
        public void scheduleRetry(UUID id, int attempts, LocalDateTime nextAttemptAt, String error) {
            outcomes.add("retry:" + attempts + ":" + nextAttemptAt + ":" + error);
        }

        @Override
        public void markFailed(UUID id, int attempts, String error, LocalDateTime at) {
            outcomes.add("failed:" + attempts + ":" + error);
        }
    }

    static final class FakeSender implements WhatsAppSenderPort {
        final List<String> calls = new ArrayList<>();
        Credentials lastCredentials;
        SendResult next = SendResult.ok("wamid.OUT");

        @Override
        public SendResult sendMessage(Credentials credentials, String to, OutboundReply reply) {
            lastCredentials = credentials;
            calls.add("mensaje:" + to + ":" + reply.text());
            return next;
        }

        @Override
        public SendResult sendTemplate(Credentials credentials, String to, String templateName, String languageCode,
                                       List<String> parameters) {
            lastCredentials = credentials;
            calls.add("plantilla:" + to + ":" + templateName + ":" + languageCode + ":" + parameters);
            return next;
        }

        @Override
        public SendResult sendTemplate(Credentials credentials, String to, String templateName, String languageCode,
                                       List<String> parameters, List<String> buttonPayloads) {
            lastCredentials = credentials;
            calls.add("plantilla:" + to + ":" + templateName + ":" + languageCode + ":" + parameters + ":botones" + buttonPayloads);
            return next;
        }
    }
}
