package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort.DoctorContact;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Medicos que atienden pacientes. Con dos o mas, el paciente elige de una lista con ids reales. */
public final class DoctorsTool implements AgentTool {

    public static final String NAME = "medicos";
    public static final String OPTION_PREFIX = "doctor:";
    /** Si el modelo no logra redactar la pregunta, sale esta con la lista de medicos. */
    public static final String CHOOSE_DOCTOR = "¿Con cuál de nuestros médicos te gustaría agendar?";

    private final DoctorDirectoryPort doctors;

    public DoctorsTool(DoctorDirectoryPort doctors) {
        this.doctors = doctors;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Médicos de la clínica que atienden pacientes. Si hay varios, aparecen como lista para "
                + "que el paciente elija; también indica con quién fue su última cita.", List.of());
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        List<DoctorContact> all = doctors.listDoctors(context.clinicId());
        Map<String, Object> content = new LinkedHashMap<>();
        List<String> names = all.stream().map(DoctorContact::displayName).toList();
        content.put("medicos", names);
        List<String> facts = new ArrayList<>(names);
        if (context.patients().size() == 1) {
            doctors.lastDoctorOf(context.clinicId(), context.patients().getFirst().patientId()).ifPresent(last -> {
                content.put("ultimo_medico", last.displayName());
                facts.add(last.displayName());
            });
        }
        ToolOutcome outcome = new ToolOutcome(content, all.size() > 1 ? options(all) : List.of(), facts);
        return all.size() > 1 ? outcome.withFallback(CHOOSE_DOCTOR) : outcome;
    }

    static List<ConversationOption> options(List<DoctorContact> all) {
        return all.stream().map(doctor -> new ConversationOption(OPTION_PREFIX + doctor.staffId(), doctor.displayName())).toList();
    }
}
