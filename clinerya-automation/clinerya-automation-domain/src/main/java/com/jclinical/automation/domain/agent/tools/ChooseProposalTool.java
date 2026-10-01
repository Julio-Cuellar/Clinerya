package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort.SlotNoLongerAvailableException;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.service.SlotLabel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * El medico propuso otros horarios: elegir uno lo agenda directo (el medico ya lo aprobo al
 * proponerlo); "ninguno" los libera. Solo acepta las opciones que se le mostraron al paciente.
 */
public final class ChooseProposalTool implements AgentTool {

    public static final String NAME = "elegir_propuesta";
    public static final String PREFIX = "propuesta:";
    public static final String NONE = "propuesta:ninguna";

    private final AppointmentRequestPort requests;
    private final ConversationRepositoryPort conversations;

    public ChooseProposalTool(AppointmentRequestPort requests, ConversationRepositoryPort conversations) {
        this.requests = requests;
        this.conversations = conversations;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "El paciente eligió uno de los horarios que le propuso el médico, o ninguno.",
                List.of(new ToolSpec.Parameter("opcion", "string", "Id exacto de la opción (propuesta:...).", true)));
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        String chosen = ToolArgs.text(arguments, "opcion");
        boolean offered = context.offeredOptions().stream().anyMatch(option -> option.id().equals(chosen));
        Optional<Conversation> conversation = conversations.findById(context.conversationId());
        if (!offered || !chosen.startsWith(PREFIX) || conversation.isEmpty() || conversation.get().requestId() == null) {
            return ToolOutcome.of(Map.of("error", "Esa opción ya no está vigente."));
        }
        if (NONE.equals(chosen)) {
            requests.declineOptions(context.clinicId(), conversation.get().requestId());
            Conversations.setRequest(conversations, context.conversationId(), null);
            return ToolOutcome.of(Map.of("propuestas_descartadas", true));
        }
        String[] parts = chosen.substring(PREFIX.length()).split("[|]");
        LocalDateTime start = LocalDateTime.parse(parts[1]);
        LocalDateTime end = LocalDateTime.parse(parts[2]);
        try {
            requests.chooseOption(context.clinicId(), conversation.get().requestId(), start, end);
        } catch (SlotNoLongerAvailableException taken) {
            return ToolOutcome.of(Map.of("error", "Ese horario ya se ocupó; que elija otra de las opciones."));
        }
        Conversations.setRequest(conversations, context.conversationId(), null);
        String label = SlotLabel.of(start);
        return new ToolOutcome(Map.of("cita_agendada", true, "fecha", label), List.of(), List.of(label))
                .withClosing("¡Listo! Tu cita quedó agendada para el " + label + ". Te esperamos.");
    }
}
