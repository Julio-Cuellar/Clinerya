package com.jclinical.automation.infra.adapters.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.ConversationOption;

import java.util.List;

/** Serializa en JSON las opciones ofrecidas: la lista blanca contra la que se valida el siguiente mensaje. */
public class ConversationOptionsCodec {

    private static final TypeReference<List<ConversationOption>> OPTIONS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ConversationOptionsCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(List<ConversationOption> options) {
        try {
            return objectMapper.writeValueAsString(options == null ? List.of() : options);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudieron guardar las opciones de la conversación.", exception);
        }
    }

    public List<ConversationOption> decode(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, OPTIONS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Las opciones guardadas de la conversación no son válidas.", exception);
        }
    }
}
