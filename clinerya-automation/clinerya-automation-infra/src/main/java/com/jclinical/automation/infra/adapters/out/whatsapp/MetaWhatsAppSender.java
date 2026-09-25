package com.jclinical.automation.infra.adapters.out.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.ports.out.WhatsAppSenderPort;
import org.springframework.web.client.RestClient;

import java.util.List;

public class MetaWhatsAppSender implements WhatsAppSenderPort {

    public MetaWhatsAppSender(RestClient restClient, ObjectMapper objectMapper, String graphBaseUrl, String graphVersion) {
    }

    @Override
    public SendResult sendMessage(Credentials credentials, String to, OutboundReply reply) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public SendResult sendTemplate(Credentials credentials, String to, String templateName, String languageCode,
                                   List<String> parameters) {
        throw new UnsupportedOperationException("pendiente");
    }
}
