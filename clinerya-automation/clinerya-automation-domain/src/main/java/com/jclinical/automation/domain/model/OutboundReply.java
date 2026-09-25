package com.jclinical.automation.domain.model;

import java.util.List;

/** Respuesta al paciente: texto y, si aplica, las opciones que puede elegir. */
public record OutboundReply(String text, List<ConversationOption> options) {

    public OutboundReply {
        options = options == null ? List.of() : List.copyOf(options);
    }

    public static OutboundReply text(String text) {
        return new OutboundReply(text, List.of());
    }
}
