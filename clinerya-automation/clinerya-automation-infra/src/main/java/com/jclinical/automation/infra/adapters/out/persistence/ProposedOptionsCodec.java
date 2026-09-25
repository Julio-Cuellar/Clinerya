package com.jclinical.automation.infra.adapters.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jclinical.automation.domain.model.AppointmentRequest.ProposedOption;

import java.util.List;

/** Opciones que propuso el medico (cada una con su apartado), guardadas como JSON con fechas ISO. */
public class ProposedOptionsCodec {

    private static final TypeReference<List<ProposedOption>> OPTIONS_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public ProposedOptionsCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String encode(List<ProposedOption> options) {
        try {
            return objectMapper.writeValueAsString(options == null ? List.of() : options);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudieron guardar las opciones propuestas.", exception);
        }
    }

    public List<ProposedOption> decode(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(objectMapper.readValue(json, OPTIONS_TYPE));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Las opciones propuestas guardadas no son validas.", exception);
        }
    }
}
