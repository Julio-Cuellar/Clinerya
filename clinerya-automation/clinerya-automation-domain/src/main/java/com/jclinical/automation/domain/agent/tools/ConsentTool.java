package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.ClinicInfo;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.ClinicInfoPort;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort;
import com.jclinical.automation.domain.ports.out.PendingActionPort;

import java.util.List;
import java.util.Map;

/**
 * Inicia el alta de un paciente nuevo: manda la autorizacion de contacto oficial tal cual (la misma
 * que se lee en recepcion, con el aviso de privacidad) y queda esperando su respuesta.
 */
public final class ConsentTool implements AgentTool {

    public static final String NAME = "pedir_consentimiento";
    public static final String ACCEPT = "consentimiento:acepto";
    public static final String DECLINE = "consentimiento:no";
    static final List<ConversationOption> OPTIONS = List.of(
            new ConversationOption(ACCEPT, "Acepto"), new ConversationOption(DECLINE, "No acepto"));

    private final PatientRegistrationPort registrations;
    private final ClinicInfoPort clinics;
    private final PendingActionPort pending;

    public ConsentTool(PatientRegistrationPort registrations, ClinicInfoPort clinics, PendingActionPort pending) {
        this.registrations = registrations;
        this.clinics = clinics;
        this.pending = pending;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Inicia el registro de alguien que aún no es paciente (por ejemplo, para agendar). "
                + "Envía la autorización de contacto oficial con botones; tú solo escribe una frase breve que la presente.", List.of());
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        if (!context.patients().isEmpty()) {
            return ToolOutcome.of(Map.of("error", "Quien escribe ya está registrado como paciente."));
        }
        String text = registrations.consentText().text();
        String privacy = clinics.find(context.clinicId()).map(ClinicInfo::privacyNoticeUrl)
                .filter(url -> url != null && !url.isBlank()).orElse(null);
        String verbatim = privacy == null ? text : text + "\n\nAviso de privacidad: " + privacy.trim();
        pending.save(new PendingAction(context.conversationId(), PendingAction.Kind.CONSENT, null, null, null, null,
                null, null, null, context.now()));
        return ToolOutcome.of(Map.of("autorizacion_enviada", true,
                        "nota", "El texto oficial y los botones ya van en el mensaje; no lo repitas ni lo resumas."))
                .withOptions(OPTIONS).withVerbatim(verbatim);
    }
}
