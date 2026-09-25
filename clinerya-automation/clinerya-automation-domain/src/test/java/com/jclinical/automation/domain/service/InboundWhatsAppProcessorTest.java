package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage.Kind;
import com.jclinical.automation.domain.model.WhatsAppMessageReceivedEvent;
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
    private List<OutboundReply> replies = List.of();

    private final InboundWhatsAppProcessor processor = new InboundWhatsAppProcessor(
            message -> {
                handled.add(message);
                return replies;
            },
            queued::add);

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

    private WhatsAppMessageReceivedEvent event(Kind kind, String text, String optionId) {
        return new WhatsAppMessageReceivedEvent(UUID.randomUUID(), clinicId, "wamid.1", "5215512345678", kind, text,
                optionId, NOW);
    }
}
