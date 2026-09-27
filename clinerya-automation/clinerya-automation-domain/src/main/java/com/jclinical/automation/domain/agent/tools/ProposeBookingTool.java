package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort.DoctorContact;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;
import com.jclinical.automation.domain.ports.out.PendingActionPort;
import com.jclinical.automation.domain.service.SlotLabel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Primer paso para agendar: deja la cita propuesta en espera de confirmacion. Solo acepta un horario
 * que se le mostro al paciente (sus ids vienen de la agenda, no del modelo).
 */
public final class ProposeBookingTool implements AgentTool {

    public static final String NAME = "proponer_cita";
    public static final String PATIENT_PREFIX = "paciente:";

    private final DoctorDirectoryPort doctors;
    private final PendingActionPort pending;

    public ProposeBookingTool(DoctorDirectoryPort doctors, PendingActionPort pending) {
        this.doctors = doctors;
        this.pending = pending;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Propone agendar un horario que el paciente eligió. No agenda: deja la cita lista "
                + "para que el paciente la confirme en su siguiente mensaje.",
                List.of(new ToolSpec.Parameter("horario", "string", "Id exacto de la opción del horario (slot:...).", true),
                        new ToolSpec.Parameter("paciente", "string",
                                "Id de la opción del paciente (paciente:...), si el celular es de varios.", false)));
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        String slotId = ToolArgs.text(arguments, "horario");
        Optional<ConversationOption> offered = context.offeredOptions().stream()
                .filter(option -> option.id().equals(slotId) && option.id().startsWith(SlotsTool.OPTION_PREFIX)).findFirst();
        if (offered.isEmpty()) {
            return ToolOutcome.of(Map.of("error", "Ese horario no está entre las opciones que vio el paciente; busca horarios de nuevo."));
        }
        if (context.patients().isEmpty()) {
            return ToolOutcome.of(Map.of("error", "Quien escribe aún no está registrado como paciente.", "registro_necesario", true));
        }
        Optional<PatientContact> patient = patient(context, ToolArgs.text(arguments, "paciente"));
        if (patient.isEmpty()) {
            return ToolOutcome.of(Map.of("error", "Hay que saber para cuál paciente es la cita.",
                            "pacientes", context.patients().stream().map(PatientContact::displayName).toList()))
                    .withOptions(context.patients().stream()
                            .map(p -> new ConversationOption(PATIENT_PREFIX + p.patientId(), p.displayName())).toList());
        }
        SlotsTool.ChosenSlot slot = SlotsTool.parse(offered.get().id());
        UUID doctorId = slot.doctorId();
        LocalDateTime start = slot.start();
        LocalDateTime end = slot.end();
        String doctorName = doctors.listDoctors(context.clinicId()).stream().filter(d -> d.staffId().equals(doctorId))
                .map(DoctorContact::displayName).findFirst().orElse("tu médico");
        pending.save(new PendingAction(context.conversationId(), PendingAction.Kind.BOOK, patient.get().patientId(),
                patient.get().displayName(), doctorId, doctorName, start, end, null, context.now()));
        String label = SlotLabel.of(start);
        String summary = label + " con " + doctorName + " para " + patient.get().displayName();
        return new ToolOutcome(Map.of("resumen", summary, "pide_confirmacion", true),
                ConfirmActionTool.CONFIRMATION_OPTIONS, List.of(label, doctorName, patient.get().displayName()));
    }

    private static Optional<PatientContact> patient(ToolContext context, String requested) {
        if (context.patients().size() == 1) {
            return Optional.of(context.patients().getFirst());
        }
        return context.patients().stream().filter(p -> (PATIENT_PREFIX + p.patientId()).equals(requested)).findFirst();
    }
}
