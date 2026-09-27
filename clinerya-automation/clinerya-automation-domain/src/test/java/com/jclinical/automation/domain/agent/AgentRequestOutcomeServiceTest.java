package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.agent.AgentConversationServiceTest.InMemoryConversations;
import com.jclinical.automation.domain.agent.tools.ChooseProposalTool;
import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent;
import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent.Outcome;
import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.PatientNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La respuesta del medico le llega al paciente en su conversacion con el agente, con datos del evento
 * (sin IA de por medio). El evento viaja por el outbox: una repeticion o uno ajeno no mueve nada.
 */
class AgentRequestOutcomeServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 27, 21, 0);
    private final UUID clinicId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();
    private final InMemoryConversations conversations = new InMemoryConversations();
    private final AgentRequestOutcomeService service = new AgentRequestOutcomeService(conversations);
    private final LocalDateTime thursday = LocalDateTime.of(2026, 10, 1, 16, 0);
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        conversation = conversations.save(new Conversation(UUID.randomUUID(), clinicId, "5215512345678", ConversationState.CONVERSANDO,
                null, null, null, null, requestId, List.of(), 0, NOW.minusHours(1), NOW.minusHours(1)));
    }

    private AppointmentRequestResolvedEvent event(Outcome outcome, String reason, List<AvailableSlot> options) {
        return new AppointmentRequestResolvedEvent(UUID.randomUUID(), clinicId, requestId, conversation.id(), outcome,
                "Dra. Beatriz Ramos", thursday, thursday.plusMinutes(30), reason, options, NOW);
    }

    @Test
    void anApprovedRequestIsConfirmedAndForgotten() {
        PatientNotification notice = service.onRequestResolved(event(Outcome.BOOKED, null, List.of())).orElseThrow();

        assertEquals(conversation.phone(), notice.phone());
        assertTrue(notice.reply().text().contains("Dra. Beatriz Ramos") && notice.reply().text().contains("Jue 01/10 16:00"),
                notice.reply().text());
        assertNull(current().requestId());
    }

    @Test
    void aRejectionExplainsWhyAndOffersToLookAgain() {
        PatientNotification notice = service.onRequestResolved(event(Outcome.REJECTED, "Estaré en congreso", List.of())).orElseThrow();

        assertTrue(notice.reply().text().contains("Estaré en congreso"), notice.reply().text());
        assertTrue(notice.reply().text().contains("otro horario"), notice.reply().text());
        assertNull(current().requestId());
    }

    @Test
    void theDoctorsProposalsAreOfferedAsAListAndRemembered() {
        LocalDateTime friday = LocalDateTime.of(2026, 10, 2, 10, 0);

        PatientNotification notice = service.onRequestResolved(
                event(Outcome.OPTIONS_PROPOSED, null, List.of(new AvailableSlot(friday, friday.plusMinutes(30))))).orElseThrow();

        List<ConversationOption> expected = List.of(
                new ConversationOption("propuesta:" + requestId + "|" + friday + "|" + friday.plusMinutes(30), "Vie 02/10 10:00"),
                new ConversationOption(ChooseProposalTool.NONE, "Ninguno me funciona"));
        assertEquals(expected, notice.reply().options());
        assertEquals(expected, current().offeredOptions());
        assertEquals(requestId, current().requestId(), "la solicitud sigue viva hasta que elija");
    }

    @Test
    void anExpiredRequestIsExplained() {
        PatientNotification notice = service.onRequestResolved(event(Outcome.EXPIRED, null, List.of())).orElseThrow();

        assertTrue(notice.reply().text().contains("Jue 01/10 16:00"), notice.reply().text());
        assertNull(current().requestId());
    }

    @Test
    void aRepeatedOrForeignEventMovesNothing() {
        service.onRequestResolved(event(Outcome.BOOKED, null, List.of()));

        Optional<PatientNotification> again = service.onRequestResolved(event(Outcome.BOOKED, null, List.of()));

        assertTrue(again.isEmpty());
    }

    private Conversation current() {
        return conversations.findById(conversation.id()).orElseThrow();
    }
}
