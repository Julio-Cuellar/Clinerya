package com.jclinical.automation.infra.adapters.in.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage;
import com.jclinical.automation.domain.ports.out.WebhookPayloadParserPort;

import java.time.ZoneId;
import java.util.List;

public class MetaWebhookPayloadParser implements WebhookPayloadParserPort {

    public MetaWebhookPayloadParser(ObjectMapper objectMapper, ZoneId zone) {
    }

    @Override
    public List<WhatsAppInboundMessage> parse(byte[] rawBody) {
        throw new UnsupportedOperationException("pendiente");
    }
}
