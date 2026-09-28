package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.ClinicInfo;
import com.jclinical.automation.domain.model.PromptMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Quien contesta el WhatsApp de cada clinica: el nombre de la clinica sale de su perfil publico, el
 * del asistente de lo que la clinica configuro y el tono de su prompt, solo si eligio uno propio.
 */
class AgentPersonasTest {

    private final UUID clinicId = UUID.randomUUID();

    @Test
    void theClinicsNameAssistantAndOwnPromptMakeThePersona() {
        AgentPersonas personas = new AgentPersonas(clinic -> Optional.of(info("Clínica Sonrisa")),
                clinic -> new AssistantProfile("Sofi", null, false),
                clinic -> Optional.of(settings(PromptMode.CUSTOM, "Cálida y breve.")));

        assertEquals(new AgentPersona("Clínica Sonrisa", "Sofi", "Cálida y breve."), personas.apply(clinicId));
    }

    @Test
    void aStoredPromptIsIgnoredWhileTheClinicUsesTheDefaultOne() {
        AgentPersonas personas = new AgentPersonas(clinic -> Optional.of(info("Clínica Sonrisa")),
                clinic -> AssistantProfile.EMPTY,
                clinic -> Optional.of(settings(PromptMode.DEFAULT, "Texto viejo")));

        assertEquals(new AgentPersona("Clínica Sonrisa", null, null), personas.apply(clinicId));
    }

    @Test
    void withoutPublicProfileOrSettingsItStillSpeaksForTheClinic() {
        AgentPersonas personas = new AgentPersonas(clinic -> Optional.empty(), clinic -> AssistantProfile.EMPTY,
                clinic -> Optional.empty());

        assertEquals(new AgentPersona(AgentPersonas.UNNAMED_CLINIC, null, null), personas.apply(clinicId));
    }

    private ClinicInfo info(String name) {
        return new ClinicInfo(name, null, null, null, null, List.of());
    }

    private ChannelSettings settings(PromptMode mode, String prompt) {
        return ChannelSettings.unconfigured(clinicId).toBuilder().promptMode(mode).customPrompt(prompt).build();
    }
}
