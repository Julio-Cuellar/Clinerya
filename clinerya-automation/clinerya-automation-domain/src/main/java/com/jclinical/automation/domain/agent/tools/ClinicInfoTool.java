package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.model.ClinicInfo;
import com.jclinical.automation.domain.ports.out.AssistantProfilePort;
import com.jclinical.automation.domain.ports.out.ClinicInfoPort;
import com.jclinical.automation.domain.service.ClinicInfoMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Datos publicos de la clinica, su horario y las preguntas frecuentes que escribio la clinica. */
public final class ClinicInfoTool implements AgentTool {

    public static final String NAME = "info_clinica";

    private final ClinicInfoPort clinics;
    private final AssistantProfilePort profiles;

    public ClinicInfoTool(ClinicInfoPort clinics, AssistantProfilePort profiles) {
        this.clinics = clinics;
        this.profiles = profiles;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Datos de la clínica: nombre, dirección, teléfono, correo, horario de atención y "
                + "preguntas frecuentes (formas de pago, estacionamiento, etc.).", List.of());
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        Optional<ClinicInfo> found = clinics.find(context.clinicId());
        if (found.isEmpty()) {
            return ToolOutcome.of(Map.of("error", "No encontré los datos de la clínica."));
        }
        ClinicInfo info = found.get();
        Map<String, Object> content = new LinkedHashMap<>();
        List<String> facts = new ArrayList<>();
        put(content, facts, "nombre", info.name());
        put(content, facts, "direccion", info.address());
        put(content, facts, "telefono", info.phone());
        put(content, facts, "correo", info.email());
        List<String> schedule = ClinicInfoMessage.scheduleLines(info.hours());
        if (!schedule.isEmpty()) {
            content.put("horario", schedule);
            facts.addAll(schedule);
        }
        AssistantProfile profile = profiles.find(context.clinicId());
        put(content, facts, "preguntas_frecuentes", profile == null ? null : profile.faq());
        return new ToolOutcome(content, List.of(), facts);
    }

    private static void put(Map<String, Object> content, List<String> facts, String key, String value) {
        if (value != null && !value.isBlank()) {
            content.put(key, value.trim());
            facts.add(value.trim());
        }
    }
}
