package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.AppointmentCancellationPort;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort;
import com.jclinical.automation.domain.ports.out.DoctorAlertPort;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort.NewAppointmentRequest;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort.SlotNoLongerAvailableException;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.PendingActionPort;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Segundo paso: ejecuta la accion pendiente, solo si el paciente respondio en un mensaje posterior a
 * la propuesta y antes de que venza. Agendar envia la solicitud al medico (el la aprueba).
 */
public final class ConfirmActionTool implements AgentTool {

    public static final String NAME = "confirmar_accion";
    public static final String CONFIRM = "accion:confirmar";
    public static final String CHANGE = "accion:cambiar";
    public static final List<ConversationOption> CONFIRMATION_OPTIONS = List.of(
            new ConversationOption(CONFIRM, "Sí, confírmalo"), new ConversationOption(CHANGE, "Cambiar"));
    static final Duration PROPOSAL_TTL = Duration.ofMinutes(30);

    private final PendingActionPort pending;
    private final AppointmentRequestPort requests;
    private final ConversationRepositoryPort conversations;
    private final AppointmentCancellationPort cancellation;
    private final DoctorAlertPort alerts;

    public ConfirmActionTool(PendingActionPort pending, AppointmentRequestPort requests, ConversationRepositoryPort conversations,
                             AppointmentCancellationPort cancellation, DoctorAlertPort alerts) {
        this.pending = pending;
        this.requests = requests;
        this.conversations = conversations;
        this.cancellation = cancellation;
        this.alerts = alerts;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Ejecuta la acción que se le propuso al paciente, solo después de que él la confirme "
                + "de forma explícita en su mensaje.", List.of());
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        Optional<PendingAction> found = pending.find(context.conversationId());
        if (found.isEmpty()) {
            return ToolOutcome.of(Map.of("error", "No hay nada pendiente de confirmar."));
        }
        PendingAction action = found.get();
        if (!action.proposedAt().isBefore(context.now())) {
            return ToolOutcome.of(Map.of("error", "El paciente todavía no confirma; espera su respuesta."));
        }
        pending.clear(context.conversationId());
        if (action.proposedAt().plus(PROPOSAL_TTL).isBefore(context.now())) {
            return ToolOutcome.of(Map.of("error", "La propuesta venció; vuelve a proponer el horario."));
        }
        return switch (action.kind()) {
            case BOOK -> submit(context, action, null,
                    "La cita queda en revisión del médico; el paciente recibirá la respuesta por este chat.");
            case RESCHEDULE -> submit(context, action, action.appointmentId(),
                    "La cita actual se conserva hasta que el médico apruebe el cambio; la respuesta llegará por este chat.");
            case CANCEL -> cancel(context, action);
            default -> ToolOutcome.of(Map.of("error", "No hay nada pendiente de confirmar."));
        };
    }

    private ToolOutcome submit(ToolContext context, PendingAction action, UUID replaces, String note) {
        try {
            UUID requestId = requests.submit(new NewAppointmentRequest(context.clinicId(), context.conversationId(),
                    action.patientId(), action.doctorStaffId(), action.start(), action.end(), context.phone(),
                    action.patientName(), action.doctorName(), replaces, action.serviceId()));
            Conversations.setRequest(conversations, context.conversationId(), requestId);
        } catch (SlotNoLongerAvailableException taken) {
            return ToolOutcome.of(Map.of("error", "Ese horario se acaba de ocupar; hay que buscar otro."));
        }
        return ToolOutcome.of(Map.of("solicitud_enviada", true, "nota", note));
    }

    private ToolOutcome cancel(ToolContext context, PendingAction action) {
        try {
            cancellation.cancel(context.clinicId(), action.appointmentId(), action.patientId(), action.note());
        } catch (AppointmentCancellationPort.NotCancellableException gone) {
            return ToolOutcome.of(Map.of("error", "Esa cita ya no se puede cancelar por aquí."));
        }
        alerts.appointmentCancelled(context.clinicId(), action.doctorStaffId(), action.start());
        return ToolOutcome.of(Map.of("cita_cancelada", true));
    }
}
