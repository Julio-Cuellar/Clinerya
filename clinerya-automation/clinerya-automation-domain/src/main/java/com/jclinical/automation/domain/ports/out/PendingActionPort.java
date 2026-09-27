package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.PendingAction;

import java.util.Optional;
import java.util.UUID;

/** La accion que espera confirmacion en una conversacion (a lo mas una; proponer otra la reemplaza). */
public interface PendingActionPort {

    void save(PendingAction action);

    Optional<PendingAction> find(UUID conversationId);

    void clear(UUID conversationId);
}
