package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.ConversationOption;

import java.util.List;
import java.util.Optional;

/**
 * Traduce el texto libre del paciente a una de las opciones ofrecidas. Nunca decide nada por su
 * cuenta: el motor descarta cualquier respuesta que no sea el id de una opcion ofrecida.
 */
public interface IntentInterpreterPort {
    Optional<String> interpret(String text, List<ConversationOption> options);
}
