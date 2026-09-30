package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.ports.in.ManageAssistantProfileUseCase;
import com.jclinical.automation.domain.service.AssistantProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Guardar el perfil y registrarlo en la bitacora van juntos. */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalAssistantProfileUseCase implements ManageAssistantProfileUseCase {

    private final AssistantProfileService profiles;

    @Override
    @Transactional(readOnly = true)
    public AssistantProfile get(UUID actingUserId, UUID clinicId) {
        return profiles.get(actingUserId, clinicId);
    }

    @Override
    @Transactional
    public AssistantProfile update(UUID actingUserId, UUID clinicId, AssistantProfile profile) {
        return profiles.update(actingUserId, clinicId, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean assistantEnabled(UUID actingUserId, UUID clinicId) {
        return profiles.assistantEnabled(actingUserId, clinicId);
    }
}
