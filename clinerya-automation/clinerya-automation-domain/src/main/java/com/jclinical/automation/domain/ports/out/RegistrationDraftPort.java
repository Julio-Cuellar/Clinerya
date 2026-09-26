package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.RegistrationDraft;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationDraftPort {

    Optional<RegistrationDraft> find(UUID conversationId);

    void save(RegistrationDraft draft);

    void delete(UUID conversationId);
}
