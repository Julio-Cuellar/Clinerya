package com.jclinical.automation.infra.adapters.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.AppointmentRequest.ProposedOption;

import java.util.List;

public class ProposedOptionsCodec {

    public ProposedOptionsCodec(ObjectMapper objectMapper) {
    }

    public String encode(List<ProposedOption> options) {
        throw new UnsupportedOperationException("pendiente");
    }

    public List<ProposedOption> decode(String json) {
        throw new UnsupportedOperationException("pendiente");
    }
}
