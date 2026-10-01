package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.agent.tools.ConfirmActionTool;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.PendingActionPort;
import com.jclinical.automation.domain.service.SlotLabel;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Lo que toca el paciente en "Si, confirmalo" / "Cambiar" cuando hay una accion pendiente (agendar,
 * reprogramar, cancelar). Confirmar la ejecuta el codigo con la misma herramienta de siempre y el texto
 * lo escribe ella; cambiar descarta la propuesta y deja que el agente continue. Nada de esto depende de
 * que el modelo llame a la herramienta correcta. Tambien sabe volver a preguntar la confirmacion si el
 * modelo falla con una accion esperando.
 */
public final class ConfirmationReplyHandler {

    static final String GENERIC_FAILURE = "No pude completar eso. ¿Me lo pides de nuevo, por favor?";
    private static final String CHANGE_REQUEST = "Quiero cambiar lo que me propusiste.";
    private static final Set<PendingAction.Kind> CONFIRMABLE = Set.of(
            PendingAction.Kind.BOOK, PendingAction.Kind.RESCHEDULE, PendingAction.Kind.CANCEL);

    private final PendingActionPort pending;
    private final ConfirmActionTool confirm;

    public ConfirmationReplyHandler(PendingActionPort pending, ConfirmActionTool confirm) {
        this.pending = pending;
        this.confirm = confirm;
    }

    /** @return vacio si no es un boton de confirmacion o no hay nada que confirmar (lo ve el agente como siempre) */
    public Optional<ReminderReplyHandler.Outcome> handle(ToolContext context, String optionId) {
        boolean confirming = ConfirmActionTool.CONFIRM.equals(optionId);
        if (!confirming && !ConfirmActionTool.CHANGE.equals(optionId)) {
            return Optional.empty();
        }
        if (waiting(context.conversationId()).isEmpty()) {
            return Optional.empty();
        }
        if (!confirming) {
            pending.clear(context.conversationId());
            return Optional.of(new ReminderReplyHandler.Outcome.ForAgent(CHANGE_REQUEST));
        }
        ToolOutcome outcome = confirm.run(context, Map.of());
        String text = outcome.closing() != null ? outcome.closing() : GENERIC_FAILURE;
        return Optional.of(new ReminderReplyHandler.Outcome.Reply(OutboundReply.text(text)));
    }

    /** La pregunta de confirmacion de lo que sigue pendiente (y vigente), con sus botones. */
    public Optional<OutboundReply> pendingPrompt(UUID conversationId, LocalDateTime now) {
        return waiting(conversationId)
                .filter(action -> !action.proposedAt().plus(ConfirmActionTool.PROPOSAL_TTL).isBefore(now))
                .map(action -> new OutboundReply("Perdona, se me cortó la respuesta. " + question(action),
                        ConfirmActionTool.CONFIRMATION_OPTIONS));
    }

    private Optional<PendingAction> waiting(UUID conversationId) {
        return pending.find(conversationId).filter(action -> CONFIRMABLE.contains(action.kind()));
    }

    private static String question(PendingAction action) {
        String label = SlotLabel.of(action.start()) + " con " + action.doctorName();
        return switch (action.kind()) {
            case CANCEL -> "¿Confirmas que quieres cancelar tu cita del " + label + "?";
            case RESCHEDULE -> "¿Confirmas el cambio de tu cita al " + label + "?";
            default -> "¿Confirmas tu cita del " + label + "?";
        };
    }
}
