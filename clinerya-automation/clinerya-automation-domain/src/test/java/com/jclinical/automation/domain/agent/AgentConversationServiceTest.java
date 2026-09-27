package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.agent.AgentMessage.Assistant;
import com.jclinical.automation.domain.agent.AgentMessage.ToolCall;
import com.jclinical.automation.domain.agent.AgentMessage.User;
import com.jclinical.automation.domain.agent.ConversationAgentTest.RecordingTool;
import com.jclinical.automation.domain.agent.ConversationAgentTest.ScriptedModel;
import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La conversacion con el paciente conducida por el agente: memoria del chat, opciones que se tocan
 * o se escriben, y la regla de la clinica para cuando no se entiende (3 veces seguidas: a una persona).
 */
class AgentConversationServiceTest {

    private static final String PHONE = "5215512345678";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 27, 21, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final InMemoryConversations conversations = new InMemoryConversations();
    private final InMemoryHistory history = new InMemoryHistory();
    private final List<PatientDirectoryPort.PatientContact> patients = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private AgentConversationService service(ScriptedModel model, AgentTool... tools) {
        return new AgentConversationService(conversations, history, (clinic, phone) -> patients,
                clinic -> new AgentPersona("Clínica Sonrisa", null, null), new ConversationAgent(model, List.of(tools)), clock);
    }

    private List<OutboundReply> send(AgentConversationService service, String text) {
        return send(service, text, null);
    }

    private List<OutboundReply> send(AgentConversationService service, String text, String optionId) {
        history.record(new ChatMessage(UUID.randomUUID(), clinicId, PHONE, ChatMessage.Direction.INBOUND,
                text == null ? optionId : text, List.of(), NOW));
        return service.handle(new InboundMessage(clinicId, PHONE, text, optionId, NOW));
    }

    @Test
    void theAgentSeesTheRecentChatInOrderPlusTheNewMessage() {
        history.add(ChatMessage.Direction.INBOUND, "hola", NOW.minusMinutes(3));
        history.add(ChatMessage.Direction.OUTBOUND, "¡Hola! ¿En qué te ayudo?", NOW.minusMinutes(2));
        history.add(ChatMessage.Direction.INBOUND, "mensaje de ayer", NOW.minusHours(13));
        ScriptedModel model = new ScriptedModel(new ModelStep.Reply(List.of("Estamos en el centro.")));

        List<OutboundReply> replies = send(service(model), "¿dónde están?");

        assertEquals(List.of(new User("hola"), new Assistant("¡Hola! ¿En qué te ayudo?"), new User("¿dónde están?")),
                model.transcripts.get(0), "sin repetir el mensaje actual y sin lo de hace mas de 12 h");
        assertEquals(List.of(OutboundReply.text("Estamos en el centro.")), replies);
    }

    @Test
    void itIntroducesTheClinicThroughTheSystemInstruction() {
        CapturingModel model = new CapturingModel();

        send(new AgentConversationService(conversations, history, (clinic, phone) -> patients,
                clinic -> new AgentPersona("Clínica Sonrisa", null, null), new ConversationAgent(model, List.of()), clock), "hola");

        assertTrue(model.systemInstruction.contains("Clínica Sonrisa"), model.systemInstruction);
    }

    @Test
    void theAgentKnowsWhoIsWritingAndWhatDayItIs() {
        patients.add(new PatientDirectoryPort.PatientContact(UUID.randomUUID(), "Ana López"));
        CapturingModel model = new CapturingModel();

        send(new AgentConversationService(conversations, history, (clinic, phone) -> patients,
                clinic -> new AgentPersona("Clínica Sonrisa", null, null), new ConversationAgent(model, List.of()), clock), "hola");

        assertTrue(model.systemInstruction.contains("Ana López"), model.systemInstruction);
        assertTrue(model.systemInstruction.contains("27 de septiembre de 2026"), model.systemInstruction);
    }

    @Test
    void whatAToolSavesInTheConversationIsNotOverwrittenByTheTurn() {
        UUID requestId = UUID.randomUUID();
        AgentTool confirm = new AgentTool() {
            @Override public ToolSpec spec() { return new ToolSpec("confirmar_accion", "Confirma", List.of()); }
            @Override public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
                Conversation current = conversations.findById(context.conversationId()).orElseThrow();
                conversations.save(new Conversation(current.id(), current.clinicId(), current.phone(), current.state(), null, null,
                        null, null, requestId, current.offeredOptions(), current.unrecognizedCount(), current.createdAt(),
                        current.lastActivityAt()));
                return ToolOutcome.of(Map.of("solicitud_enviada", true));
            }
        };
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", "confirmar_accion", Map.of()))),
                new ModelStep.Reply(List.of("¡Listo! Ya le enviamos tu solicitud a la doctora.")));

        send(service(model, confirm), "sí, agéndala");

        assertEquals(requestId, current().requestId());
    }

    @Test
    void theToolsKnowWhoIsWritingWithoutTheModelSayingIt() {
        UUID patientId = UUID.randomUUID();
        patients.add(new PatientDirectoryPort.PatientContact(patientId, "Ana López"));
        RecordingTool tool = new RecordingTool("mis_citas", ToolOutcome.of(Map.of()));
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", "mis_citas", Map.of()))),
                new ModelStep.Reply(List.of("Tu próxima cita es el lunes.")));

        send(service(model, tool), "¿cuándo es mi cita?");

        assertEquals(clinicId, tool.lastContext.clinicId());
        assertEquals(PHONE, tool.lastContext.phone());
        assertEquals(patientId, tool.lastContext.patients().getFirst().patientId());
    }

    @Test
    void offeredOptionsGoWithTheLastBubbleAndAreRemembered() {
        List<ConversationOption> slots = List.of(new ConversationOption("slot:a", "Jue 16:00"), new ConversationOption("slot:b", "Jue 16:30"));
        RecordingTool tool = new RecordingTool("buscar_horarios", ToolOutcome.of(Map.of()).withOptions(slots));
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", "buscar_horarios", Map.of()))),
                new ModelStep.Reply(List.of("¡Claro!", "Tengo estos horarios el jueves 👇")));

        List<OutboundReply> replies = send(service(model, tool), "quiero cita el jueves en la tarde");

        assertEquals(OutboundReply.text("¡Claro!"), replies.get(0));
        assertEquals(new OutboundReply("Tengo estos horarios el jueves 👇", slots), replies.get(1));
        assertEquals(slots, current().offeredOptions());
    }

    @Test
    void aTappedOptionReachesTheAgentWithItsLabelAndId() {
        conversations.save(conversation(ConversationState.CONVERSANDO, List.of(new ConversationOption("slot:a", "Jue 16:00")), 0));
        ScriptedModel model = new ScriptedModel(new ModelStep.Reply(List.of("Perfecto, ¿lo confirmo?")));

        send(service(model), null, "slot:a");

        String last = ((User) model.transcripts.get(0).getLast()).text();
        assertTrue(last.contains("Jue 16:00") && last.contains("slot:a"), last);
    }

    @Test
    void aTappedOptionThatIsNoLongerOfferedIsNotPassedOffAsValid() {
        conversations.save(conversation(ConversationState.CONVERSANDO, List.of(), 0));
        ScriptedModel model = new ScriptedModel(new ModelStep.Reply(List.of("Ese horario ya no está disponible.")));

        send(service(model), null, "slot:viejo");

        String last = ((User) model.transcripts.get(0).getLast()).text();
        assertTrue(last.contains(AgentConversationService.STALE_OPTION), last);
    }

    @Test
    void afterThreeMisunderstandingsInARowAPersonTakesOver() {
        ScriptedModel model = new ScriptedModel(
                notUnderstood(), new ModelStep.Reply(List.of("Perdona, ¿me lo explicas de otra forma?")),
                notUnderstood(), new ModelStep.Reply(List.of("Creo que no te entendí, ¿qué necesitas?")),
                notUnderstood(), new ModelStep.Reply(List.of("¿Otra vez?")));
        AgentConversationService service = service(model);

        send(service, "asdf");
        send(service, "qwer");
        List<OutboundReply> third = send(service, "zxcv");

        assertEquals(List.of(OutboundReply.text(AgentConversationService.HANDOFF_REPLY)), third);
        assertEquals(ConversationState.ATENCION_HUMANA, current().state());
    }

    @Test
    void understandingSomethingResetsTheCount() {
        ScriptedModel model = new ScriptedModel(
                notUnderstood(), new ModelStep.Reply(List.of("¿Me lo repites?")),
                notUnderstood(), new ModelStep.Reply(List.of("¿Otra forma?")),
                new ModelStep.Reply(List.of("¡Ah, ya! Estamos en el centro.")),
                notUnderstood(), new ModelStep.Reply(List.of("¿Perdón?")));
        AgentConversationService service = service(model);

        send(service, "asdf");
        send(service, "qwer");
        send(service, "¿dónde están?");
        send(service, "zxcv");

        assertEquals(ConversationState.CONVERSANDO, current().state());
        assertEquals(1, current().unrecognizedCount());
    }

    @Test
    void whileAPersonAttendsTheChatTheAgentStaysSilent() {
        conversations.save(conversation(ConversationState.ATENCION_HUMANA, List.of(), 0));
        ScriptedModel model = new ScriptedModel(new ModelStep.Reply(List.of("No debería hablar")));

        List<OutboundReply> replies = send(service(model), "¿hola?");

        assertTrue(replies.isEmpty());
        assertTrue(model.transcripts.isEmpty());
    }

    @Test
    void whenTheModelIsDownThePatientIsToldAndAPersonTakesOver() {
        ScriptedModel model = new ScriptedModel(new IllegalStateException("503"), new IllegalStateException("503"));

        List<OutboundReply> replies = send(service(model), "hola");

        assertEquals(List.of(OutboundReply.text(ConversationAgent.UNAVAILABLE_REPLY)), replies);
        assertEquals(ConversationState.ATENCION_HUMANA, current().state());
    }

    @Test
    void theAgentCanHandTheChatToAPersonWhenAsked() {
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", ConversationAgent.HANDOFF, Map.of()))),
                new ModelStep.Reply(List.of("Claro, le aviso a alguien de la clínica para que te atienda por aquí.")));

        List<OutboundReply> replies = send(service(model), "quiero hablar con una persona");

        assertEquals(List.of(OutboundReply.text("Claro, le aviso a alguien de la clínica para que te atienda por aquí.")), replies);
        assertEquals(ConversationState.ATENCION_HUMANA, current().state());
    }

    @Test
    void anIdleConversationStartsOverButKeepsTheChatMemory() {
        Conversation old = new Conversation(UUID.randomUUID(), clinicId, PHONE, ConversationState.CONVERSANDO, null, null, null, null,
                null, List.of(new ConversationOption("slot:a", "Jue 16:00")), 2, NOW.minusHours(2), NOW.minusHours(2));
        conversations.save(old);
        history.add(ChatMessage.Direction.INBOUND, "hola", NOW.minusHours(2));
        ScriptedModel model = new ScriptedModel(new ModelStep.Reply(List.of("¡Hola de nuevo!")));

        send(service(model), "hola otra vez");

        assertEquals(0, current().unrecognizedCount());
        assertTrue(current().offeredOptions().isEmpty());
        assertEquals(new User("hola"), model.transcripts.get(0).getFirst());
    }

    // ---- utilidades --------------------------------------------------------------------------

    private static ModelStep notUnderstood() {
        return new ModelStep.CallTools(List.of(new ToolCall("n", ConversationAgent.NOT_UNDERSTOOD, Map.of())));
    }

    private Conversation conversation(ConversationState state, List<ConversationOption> options, int unrecognized) {
        return new Conversation(UUID.randomUUID(), clinicId, PHONE, state, null, null, null, null, null, options, unrecognized,
                NOW.minusMinutes(5), NOW.minusMinutes(5));
    }

    private Conversation current() {
        return conversations.findActive(clinicId, PHONE).orElseThrow();
    }

    static final class CapturingModel implements ConversationModelPort {
        String systemInstruction;

        @Override
        public ModelStep next(UUID clinicId, String systemInstruction, List<AgentMessage> transcript, List<ToolSpec> tools) {
            this.systemInstruction = systemInstruction;
            return new ModelStep.Reply(List.of("Hola"));
        }
    }

    static final class InMemoryConversations implements ConversationRepositoryPort {
        final List<Conversation> all = new ArrayList<>();

        @Override
        public Optional<Conversation> findActive(UUID clinicId, String phone) {
            return all.stream().filter(c -> c.clinicId().equals(clinicId) && c.phone().equals(phone) && !c.state().isTerminal())
                    .max(Comparator.comparing(Conversation::createdAt));
        }

        @Override
        public Optional<Conversation> findById(UUID conversationId) {
            return all.stream().filter(c -> c.id().equals(conversationId)).findFirst();
        }

        @Override
        public Conversation save(Conversation conversation) {
            all.removeIf(existing -> existing.id().equals(conversation.id()));
            all.add(conversation);
            return conversation;
        }
    }

    final class InMemoryHistory implements ChatHistoryPort {
        final List<ChatMessage> messages = new ArrayList<>();

        void add(ChatMessage.Direction direction, String text, LocalDateTime at) {
            record(new ChatMessage(UUID.randomUUID(), clinicId, PHONE, direction, text, List.of(), at));
        }

        @Override public void record(ChatMessage message) { messages.add(message); }

        @Override public List<ChatSummary> findChats(UUID clinicId, int limit) { return List.of(); }

        @Override
        public List<ChatMessage> findMessages(UUID clinicId, String phone, LocalDateTime before, int limit) {
            return messages.stream().filter(m -> m.phone().equals(phone) && (before == null || m.at().isBefore(before)))
                    .sorted(Comparator.comparing(ChatMessage::at).reversed()).limit(limit).toList();
        }

        @Override
        public List<ChatMessage> findMessagesAfter(UUID clinicId, String phone, LocalDateTime after, int limit) {
            return List.of();
        }

        @Override public int deleteOlderThan(UUID clinicId, LocalDateTime cutoff) { return 0; }
    }
}
