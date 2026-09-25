package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage.Kind;
import com.jclinical.automation.domain.model.WhatsAppMessageReceivedEvent;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El mensaje aceptado pasa a la conversacion y sus respuestas quedan en cola para enviarse. */
class InboundWhatsAppProcessorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 10, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final List<InboundMessage> handled = new ArrayList<>();
    private final List<PatientNotification> queued = new ArrayList<>();
    private final List<ChatMessage> recorded = new ArrayList<>();
    private List<OutboundReply> replies = List.of();
    private final List<String> doctorPhones = new ArrayList<>();

    private final InboundWhatsAppProcessor processor = new InboundWhatsAppProcessor(
            message -> {
                handled.add(message);
                return replies;
            },
            queued::add,
            new RecordingHistory(),
            (clinic, phone) -> doctorPhones.contains(phone));

    @Test
    void textGoesToTheConversationAndEveryReplyIsQueuedInOrder() {
        replies = List.of(OutboundReply.text("Hola, Ana."),
                new OutboundReply("¿Qué deseas hacer?", List.of(new ConversationOption("action:book", "Agendar una cita"))));

        processor.process(event(Kind.TEXT, "Hola", null));

        assertEquals(List.of(new InboundMessage(clinicId, "5215512345678", "Hola", null, NOW)), handled);
        assertEquals(2, queued.size());
        assertEquals(new PatientNotification(clinicId, "5215512345678", replies.get(0)), queued.get(0));
        assertEquals("¿Qué deseas hacer?", queued.get(1).reply().text());
    }

    @Test
    void aChosenOptionTravelsAsTheSelectedOption() {
        processor.process(event(Kind.OPTION, "Agendar una cita", "action:book"));

        assertEquals("action:book", handled.get(0).selectedOptionId());
    }

    @Test
    void anUnsupportedMessageGetsAPoliteNoticeAndDoesNotMoveTheConversation() {
        processor.process(event(Kind.UNSUPPORTED, null, null));

        assertTrue(handled.isEmpty());
        assertEquals(1, queued.size());
        assertTrue(queued.get(0).reply().text().contains("texto"), queued.get(0).reply().text());
    }

    @Test
    void whatThePatientWroteIsKeptInTheChatHistory() {
        processor.process(event(Kind.TEXT, "Hola", null));
        processor.process(event(Kind.OPTION, "Agendar una cita", "action:book"));
        processor.process(event(Kind.UNSUPPORTED, null, null));

        assertEquals(List.of("Hola", "Agendar una cita", "[Mensaje que no es texto]"),
                recorded.stream().map(ChatMessage::text).toList());
        assertTrue(recorded.stream().allMatch(message -> message.direction() == ChatMessage.Direction.INBOUND));
        assertEquals("5215512345678", recorded.get(0).phone());
        assertEquals(NOW, recorded.get(0).at());
    }

    @Test
    void aDoctorWritingOnWhatsAppDoesNotStartAPatientConversation() {
        doctorPhones.add("5215512345678");

        processor.process(event(Kind.TEXT, "Acepto la cita", null));

        assertTrue(handled.isEmpty(), "el medico responde en Clinerya, no por el chat");
        assertTrue(queued.isEmpty(), "el aviso al medico no sale como mensaje de paciente");
        assertTrue(recorded.isEmpty(), "no entra al historial de pacientes");
    }

    private final class RecordingHistory implements ChatHistoryPort {
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

    private WhatsAppMessageReceivedEvent event(Kind kind, String text, String optionId) {
        return new WhatsAppMessageReceivedEvent(UUID.randomUUID(), clinicId, "wamid.1", "5215512345678", kind, text,
                optionId, NOW);
    }
}
