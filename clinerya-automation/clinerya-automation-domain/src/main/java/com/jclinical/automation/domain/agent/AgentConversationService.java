package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.agent.AgentMessage.Assistant;
import com.jclinical.automation.domain.agent.AgentMessage.User;
import com.jclinical.automation.domain.agent.tools.SlotsTool;
import com.jclinical.automation.domain.model.ChatAttentionEvent;
import com.jclinical.automation.domain.model.ChatAttentionEvent.Action;
import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.ports.out.ChatAttentionLogPort;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.ports.in.HandleInboundMessageUseCase;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * La conversacion con el paciente conducida por el agente. El codigo guarda lo minimo: las opciones
 * ofrecidas (para validar lo que se toque), cuantas veces seguidas no se entendio (3: pasa a una
 * persona) y si el chat esta en atencion humana (el agente calla). La memoria es el propio chat.
 */
public final class AgentConversationService implements HandleInboundMessageUseCase {

    public static final int MAX_NOT_UNDERSTOOD = 3;
    public static final String HANDOFF_REPLY = "Perdona, no logro entenderte bien por aquí. "
            + "Ya le avisé a alguien de la clínica para que te atienda en este mismo chat.";
    public static final String STALE_OPTION = "(esa opción ya no está vigente)";

    static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);
    /** Un chat en atencion humana que nadie regreso vuelve al agente tras este lapso sin actividad. */
    public static final Duration HUMAN_ATTENTION_TIMEOUT = Duration.ofHours(24);
    static final Duration MEMORY = Duration.ofHours(12);
    static final int MEMORY_MESSAGES = 20;

    private final ConversationRepositoryPort conversations;
    private final ChatHistoryPort history;
    private final PatientDirectoryPort patients;
    private final Function<UUID, AgentPersona> personas;
    private final ConversationAgent agent;
    private final Clock clock;
    private final ChatAttentionLogPort attentionLog;

    public AgentConversationService(ConversationRepositoryPort conversations, ChatHistoryPort history,
                                    PatientDirectoryPort patients, Function<UUID, AgentPersona> personas,
                                    ConversationAgent agent, Clock clock, ChatAttentionLogPort attentionLog) {
        this.conversations = conversations;
        this.history = history;
        this.patients = patients;
        this.personas = personas;
        this.agent = agent;
        this.clock = clock;
        this.attentionLog = attentionLog;
    }

    private ReminderReplyHandler reminderReplies;
    private ConfirmationReplyHandler confirmationReplies;

    /** "Si, confirmalo" / "Cambiar": los resuelve el codigo; sin configurar, llegan al agente como cualquier opcion. */
    public void setConfirmationReplies(ConfirmationReplyHandler confirmationReplies) {
        this.confirmationReplies = confirmationReplies;
    }

    /** Botones del recordatorio de cita (S6); sin configurar, esos botones llegan al agente como cualquier opcion. */
    public void setReminderReplies(ReminderReplyHandler reminderReplies) {
        this.reminderReplies = reminderReplies;
    }

    private void logAttention(UUID clinicId, String phone, Action action, LocalDateTime at) {
        attentionLog.record(new ChatAttentionEvent(UUID.randomUUID(), clinicId, phone, action, null, at));
    }

    @Override
    public List<OutboundReply> handle(InboundMessage message) {
        LocalDateTime now = LocalDateTime.now(clock);
        Conversation conversation = activeOrNew(message.clinicId(), message.fromPhone(), now);
        if (conversation.state() == ConversationState.ATENCION_HUMANA) {
            // Lo que escribe el paciente mantiene vivo el chat que atiende una persona.
            conversations.save(copy(conversation, conversation.state(), conversation.offeredOptions(),
                    conversation.unrecognizedCount(), now));
            return List.of();
        }
        List<PatientContact> contacts = patients.findByPhone(message.clinicId(), message.fromPhone());
        Optional<ReminderReplyHandler.Outcome> reminder = reminderReplies == null ? Optional.empty()
                : reminderReplies.handle(message.clinicId(), conversation.id(), contacts, message.selectedOptionId(), now);
        if (reminder.isPresent() && reminder.get() instanceof ReminderReplyHandler.Outcome.Reply direct) {
            conversations.save(copy(conversation, ConversationState.CONVERSANDO, direct.reply().options(), 0, now));
            return List.of(direct.reply());
        }
        if (reminder.isPresent() && reminder.get() instanceof ReminderReplyHandler.Outcome.ForAgent forAgent) {
            message = new InboundMessage(message.clinicId(), message.fromPhone(), forAgent.text(), null, message.receivedAt());
        }
        // Las herramientas que dependen de lo que respondio el paciente (aceptar la autorizacion) leen su
        // mensaje real, no la interpretacion del modelo.
        String patientMessage = message.selectedOptionId() != null ? message.selectedOptionId() : message.text();
        ToolContext context = new ToolContext(message.clinicId(), conversation.id(), message.fromPhone(), contacts,
                conversation.offeredOptions(), now, patientMessage);
        Optional<ReminderReplyHandler.Outcome> confirmation = confirmationReplies == null ? Optional.empty()
                : confirmationReplies.handle(context, message.selectedOptionId());
        if (confirmation.isPresent() && confirmation.get() instanceof ReminderReplyHandler.Outcome.Reply confirmed) {
            Conversation latestAfterAction = conversations.findById(conversation.id()).orElse(conversation);
            conversations.save(copy(latestAfterAction, ConversationState.CONVERSANDO, confirmed.reply().options(), 0, now));
            return List.of(confirmed.reply());
        }
        if (confirmation.isPresent() && confirmation.get() instanceof ReminderReplyHandler.Outcome.ForAgent changed) {
            message = new InboundMessage(message.clinicId(), message.fromPhone(), changed.text(), null, message.receivedAt());
        }
        String instructions = AgentInstructions.build(personas.apply(message.clinicId()), now,
                contacts.stream().map(PatientContact::displayName).toList(), conversation.offeredOptions());
        AgentOutcome outcome = agent.run(context, instructions, transcript(message, conversation));
        if (outcome.failed() && confirmationReplies != null) {
            // Si el modelo fallo con algo esperando confirmacion, vuelven los botones de confirmar, no otra lista.
            Optional<OutboundReply> prompt = confirmationReplies.pendingPrompt(conversation.id(), now);
            if (prompt.isPresent()) {
                outcome = new AgentOutcome(List.of(prompt.get().text()), prompt.get().options(), outcome.notUnderstood(), false, true);
            }
        }

        // No entender y que el modelo falle cuentan igual: a la tercera seguida, a una persona.
        int misunderstood = outcome.notUnderstood() || outcome.failed() ? conversation.unrecognizedCount() + 1 : 0;
        boolean tooManyMisunderstandings = misunderstood >= MAX_NOT_UNDERSTOOD;
        boolean handoff = outcome.handoff() || tooManyMisunderstandings;
        List<String> bubbles = tooManyMisunderstandings && !outcome.handoff() ? List.of(HANDOFF_REPLY) : outcome.bubbles();
        List<ConversationOption> options = handoff ? List.of() : outcome.options();
        List<ConversationOption> remembered = handoff ? List.of() : withOfferedSlots(options, conversation.offeredOptions());
        // Las herramientas pueden haber guardado algo durante el turno (el id de la solicitud): se parte de lo guardado.
        Conversation latest = conversations.findById(conversation.id()).orElse(conversation);
        conversations.save(copy(latest, handoff ? ConversationState.ATENCION_HUMANA : ConversationState.CONVERSANDO,
                remembered, handoff ? 0 : misunderstood, now));
        if (handoff) {
            logAttention(message.clinicId(), message.fromPhone(), Action.REQUESTED_BY_AGENT, now);
        }
        return replies(bubbles, options);
    }

    /**
     * Tras 30 min sin actividad empieza una conversacion nueva (la memoria del chat se conserva). Un chat
     * en atencion humana espera a la persona, pero si nadie lo regresa y pasa un dia sin actividad vuelve
     * solo al agente: el paciente nunca se queda sin respuesta.
     */
    private Conversation activeOrNew(UUID clinicId, String phone, LocalDateTime now) {
        Optional<Conversation> active = conversations.findActive(clinicId, phone);
        if (active.isPresent()) {
            Conversation conversation = active.get();
            boolean human = conversation.state() == ConversationState.ATENCION_HUMANA;
            Duration timeout = human ? HUMAN_ATTENTION_TIMEOUT : IDLE_TIMEOUT;
            if (!conversation.lastActivityAt().plus(timeout).isBefore(now)) {
                return conversation;
            }
            conversations.save(copy(conversation, ConversationState.EXPIRADA, List.of(), conversation.unrecognizedCount(),
                    conversation.lastActivityAt()));
            if (human) {
                logAttention(clinicId, phone, Action.AUTO_RELEASED, now);
            }
        }
        return conversations.save(new Conversation(UUID.randomUUID(), clinicId, phone, ConversationState.CONVERSANDO,
                null, null, null, null, null, List.of(), 0, now, now));
    }

    /** Lo reciente del chat (hasta 12 h, 20 mensajes), del mas antiguo al mas nuevo, y el mensaje actual. */
    private List<AgentMessage> transcript(InboundMessage message, Conversation conversation) {
        LocalDateTime since = message.receivedAt().minus(MEMORY);
        List<ChatMessage> recent = new ArrayList<>(history.findMessages(message.clinicId(), message.fromPhone(),
                message.receivedAt(), MEMORY_MESSAGES));
        java.util.Collections.reverse(recent);
        List<AgentMessage> transcript = new ArrayList<>();
        for (ChatMessage past : recent) {
            if (past.at().isBefore(since) || past.text() == null || past.text().isBlank()) {
                continue;
            }
            transcript.add(past.direction() == ChatMessage.Direction.INBOUND ? new User(past.text()) : new Assistant(past.text()));
        }
        transcript.add(new User(current(message, conversation)));
        return List.copyOf(transcript);
    }

    /** Una opcion tocada llega con su etiqueta y su id; si ya no esta vigente, se dice asi. */
    private static String current(InboundMessage message, Conversation conversation) {
        if (message.selectedOptionId() == null) {
            return message.text() == null ? "" : message.text();
        }
        return conversation.offeredOptions().stream().filter(option -> option.id().equals(message.selectedOptionId()))
                .findFirst()
                .map(option -> "Elegí \"" + option.label() + "\" [opción " + option.id() + "]")
                .orElse("Elegí una opción " + STALE_OPTION + " [opción " + message.selectedOptionId() + "]");
    }

    /**
     * Los horarios ofrecidos siguen vigentes mientras no se ofrezcan otros: pedir datos o la autorizacion no
     * debe invalidar el horario que el paciente ya eligio (sin el, el registro no deja la cita lista para el medico).
     */
    private static List<ConversationOption> withOfferedSlots(List<ConversationOption> fresh, List<ConversationOption> previous) {
        if (fresh.stream().anyMatch(AgentConversationService::isSlot)) {
            return fresh;
        }
        List<ConversationOption> kept = new ArrayList<>(fresh);
        previous.stream().filter(AgentConversationService::isSlot).forEach(kept::add);
        return List.copyOf(kept);
    }

    private static boolean isSlot(ConversationOption option) {
        return option.id().startsWith(SlotsTool.OPTION_PREFIX);
    }

    /** Cada burbuja es un mensaje; las opciones viajan con la ultima (lista o botones de WhatsApp). */
    private static List<OutboundReply> replies(List<String> bubbles, List<ConversationOption> options) {
        List<OutboundReply> replies = new ArrayList<>();
        for (int i = 0; i < bubbles.size(); i++) {
            boolean last = i == bubbles.size() - 1;
            replies.add(new OutboundReply(bubbles.get(i), last ? options : List.of()));
        }
        return List.copyOf(replies);
    }

    private static Conversation copy(Conversation conversation, ConversationState state, List<ConversationOption> options,
                                     int unrecognized, LocalDateTime lastActivity) {
        return new Conversation(conversation.id(), conversation.clinicId(), conversation.phone(), state,
                conversation.patientId(), conversation.patientName(), conversation.doctorStaffId(), conversation.doctorName(),
                conversation.requestId(), options, unrecognized, conversation.createdAt(), lastActivity);
    }
}
