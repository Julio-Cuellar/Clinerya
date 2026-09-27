package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.PendingActionPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registra que el paciente acepto la autorizacion de contacto. Lo decide el mensaje real del paciente
 * en este turno (el boton o una respuesta afirmativa clara), no la interpretacion del modelo.
 */
public final class AcceptConsentTool implements AgentTool {

    public static final String NAME = "aceptar_consentimiento";
    private static final Set<String> YES = Set.of("acepto", "si acepto", "si", "de acuerdo", "autorizo", "si autorizo",
            "claro", "si claro", "claro que si", "ok", "esta bien");
    private static final Set<String> NO = Set.of("no", "no acepto", "no autorizo");

    private final PendingActionPort pending;

    public AcceptConsentTool(PendingActionPort pending) {
        this.pending = pending;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Úsala cuando el paciente responda a la autorización de contacto (aceptándola o no).", List.of());
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        Optional<PendingAction> waiting = pending.find(context.conversationId())
                .filter(action -> action.kind() == PendingAction.Kind.CONSENT);
        if (waiting.isEmpty()) {
            return ToolOutcome.of(Map.of("error", "No hay una autorización pendiente; primero hay que enviarla."));
        }
        String answer = ToolArgs.normalize(context.patientMessage()).replaceAll("[^a-z ]", " ").replaceAll(" +", " ").trim();
        if (ConsentTool.DECLINE.equals(context.patientMessage()) || NO.contains(answer)) {
            pending.clear(context.conversationId());
            return ToolOutcome.of(Map.of("rechazado", true,
                    "nota", "Sin autorización no se puede registrar ni agendar por aquí; ofrece llamar a la clínica."));
        }
        if (!ConsentTool.ACCEPT.equals(context.patientMessage()) && !YES.contains(answer)) {
            return ToolOutcome.of(Map.of("error", "El mensaje del paciente no es una aceptación clara; pregúntale si acepta."));
        }
        pending.save(new PendingAction(context.conversationId(), PendingAction.Kind.CONSENT_ACCEPTED, null, null, null, null,
                null, null, null, context.now()));
        return ToolOutcome.of(Map.of("aceptado", true,
                "siguiente", "Pide nombre, apellidos, fecha de nacimiento (dd/mm/aaaa), sexo y correo (opcional)."));
    }
}
