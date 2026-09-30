package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort;
import com.jclinical.automation.domain.ports.out.PendingActionPort;
import com.jclinical.automation.domain.service.SlotLabel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Primer paso para reprogramar: una cita propia a un horario ofrecido del mismo medico. La cita
 * original se conserva hasta que el medico aprueba el cambio.
 */
public final class ProposeRescheduleTool implements AgentTool {

    public static final String NAME = "proponer_reprogramacion";

    private final PatientAppointmentsPort appointments;
    private final DoctorDirectoryPort doctors;
    private final PendingActionPort pending;

    public ProposeRescheduleTool(PatientAppointmentsPort appointments, DoctorDirectoryPort doctors, PendingActionPort pending) {
        this.appointments = appointments;
        this.doctors = doctors;
        this.pending = pending;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Propone mover una cita del paciente a otro horario del mismo médico (búscalo antes con "
                + "buscar_horarios). No cambia nada: queda lista para que la confirme.",
                List.of(new ToolSpec.Parameter("cita", "string", "Id de la cita (cita:...) tomado de mis_citas.", true),
                        new ToolSpec.Parameter("horario", "string", "Id exacto del nuevo horario (slot:...).", true)));
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        Optional<OwnAppointments.Found> found = OwnAppointments.find(appointments, doctors, context, ToolArgs.text(arguments, "cita"));
        if (found.isEmpty()) {
            return ToolOutcome.of(Map.of("error", "Esa cita no está entre las próximas citas del paciente; consulta mis_citas."));
        }
        String slotId = ToolArgs.text(arguments, "horario");
        boolean offered = slotId.startsWith(SlotsTool.OPTION_PREFIX)
                && context.offeredOptions().stream().anyMatch(option -> option.id().equals(slotId));
        if (!offered) {
            return ToolOutcome.of(Map.of("error", "Ese horario no está entre las opciones que vio el paciente; busca horarios de nuevo."));
        }
        OwnAppointments.Found appointment = found.get();
        SlotsTool.ChosenSlot slot = SlotsTool.parse(slotId);
        if (!slot.doctorId().equals(appointment.visit().doctorStaffId())) {
            return ToolOutcome.of(Map.of("error", "Reprogramar es con el mismo médico; busca horarios de " + appointment.doctorName() + "."));
        }
        pending.save(new PendingAction(context.conversationId(), PendingAction.Kind.RESCHEDULE, appointment.patient().patientId(),
                appointment.patient().displayName(), slot.doctorId(), appointment.doctorName(), slot.start(), slot.end(),
                appointment.visit().appointmentId(), context.now(), null, slot.serviceId()));
        String from = SlotLabel.of(appointment.visit().start());
        String to = SlotLabel.of(slot.start());
        return new ToolOutcome(Map.of("resumen", "Mover la cita del " + from + " al " + to + " con " + appointment.doctorName(),
                "nota", "La cita actual se conserva hasta que el médico apruebe el cambio.", "pide_confirmacion", true),
                ConfirmActionTool.CONFIRMATION_OPTIONS, List.of(from, to, appointment.doctorName()));
    }
}
