package com.jclinical.automation.infra.adapters.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.ConversationOption;

import java.util.List;

public class ConversationOptionsCodec {

    public ConversationOptionsCodec(ObjectMapper objectMapper) {
    }

    public String encode(List<ConversationOption> options) {
        return "[]";
    }

    public List<ConversationOption> decode(String json) {
        return List.of();
    }
}
