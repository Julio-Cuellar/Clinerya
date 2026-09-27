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

/** Primer paso para cancelar: una cita propia y proxima queda en espera de confirmacion. */
public final class ProposeCancellationTool implements AgentTool {

    public static final String NAME = "proponer_cancelacion";

    private final PatientAppointmentsPort appointments;
    private final DoctorDirectoryPort doctors;
    private final PendingActionPort pending;

    public ProposeCancellationTool(PatientAppointmentsPort appointments, DoctorDirectoryPort doctors, PendingActionPort pending) {
        this.appointments = appointments;
        this.doctors = doctors;
        this.pending = pending;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Propone cancelar una cita del paciente. No cancela: queda lista para que la confirme.",
                List.of(new ToolSpec.Parameter("cita", "string", "Id de la cita (cita:...) tomado de mis_citas.", true),
                        new ToolSpec.Parameter("motivo", "string", "Motivo que dio el paciente, opcional.", false)));
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        Optional<OwnAppointments.Found> found = OwnAppointments.find(appointments, doctors, context, ToolArgs.text(arguments, "cita"));
        if (found.isEmpty()) {
            return ToolOutcome.of(Map.of("error", "Esa cita no está entre las próximas citas del paciente; consulta mis_citas."));
        }
        OwnAppointments.Found appointment = found.get();
        String reason = ToolArgs.text(arguments, "motivo");
        pending.save(new PendingAction(context.conversationId(), PendingAction.Kind.CANCEL, appointment.patient().patientId(),
                appointment.patient().displayName(), appointment.visit().doctorStaffId(), appointment.doctorName(),
                appointment.visit().start(), appointment.visit().end(), appointment.visit().appointmentId(), context.now(),
                reason.isEmpty() ? null : reason));
        String label = SlotLabel.of(appointment.visit().start());
        return new ToolOutcome(Map.of("resumen", "Cancelar la cita del " + label + " con " + appointment.doctorName(),
                "pide_confirmacion", true), ConfirmActionTool.CONFIRMATION_OPTIONS, List.of(label, appointment.doctorName()));
    }
}
