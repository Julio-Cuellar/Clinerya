package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.ClinicInfo;
import com.jclinical.automation.domain.model.PromptMode;
import com.jclinical.automation.domain.ports.out.AssistantProfilePort;
import com.jclinical.automation.domain.ports.out.ClinicInfoPort;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Arma la persona de cada clinica: su nombre publico, el nombre del asistente que configuro (vacio:
 * habla la clinica) y su prompt como tono, solo cuando eligio usar uno propio.
 */
public final class AgentPersonas implements Function<UUID, AgentPersona> {

    public static final String UNNAMED_CLINIC = "la clínica";

    private final ClinicInfoPort clinics;
    private final AssistantProfilePort profiles;
    private final Function<UUID, Optional<ChannelSettings>> settings;

    public AgentPersonas(ClinicInfoPort clinics, AssistantProfilePort profiles,
                         Function<UUID, Optional<ChannelSettings>> settings) {
        this.clinics = clinics;
        this.profiles = profiles;
        this.settings = settings;
    }

    @Override
    public AgentPersona apply(UUID clinicId) {
        String clinicName = clinics.find(clinicId).map(ClinicInfo::name)
                .filter(name -> !name.isBlank())
                .orElse(UNNAMED_CLINIC);
        String tone = settings.apply(clinicId)
                .filter(found -> found.promptMode() == PromptMode.CUSTOM)
                .map(ChannelSettings::customPrompt)
                .filter(prompt -> !prompt.isBlank())
                .orElse(null);
        return new AgentPersona(clinicName, profiles.find(clinicId).assistantName(), tone);
    }
}
