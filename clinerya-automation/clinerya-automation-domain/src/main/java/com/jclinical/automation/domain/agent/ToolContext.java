package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lo que el codigo sabe del chat y le entrega a cada herramienta: la clinica, el celular, los
 * pacientes con ese celular y las opciones que se le ofrecieron. El modelo no puede cambiar nada de
 * esto con sus argumentos.
 */
public record ToolContext(UUID clinicId, UUID conversationId, String phone, List<PatientContact> patients,
                          List<ConversationOption> offeredOptions, LocalDateTime now) {

    public ToolContext {
        patients = patients == null ? List.of() : List.copyOf(patients);
        offeredOptions = offeredOptions == null ? List.of() : List.copyOf(offeredOptions);
    }
}
