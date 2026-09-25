package com.jclinical.automation.infra.adapters.out.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.ports.out.ChannelConnectionCheckPort;
import org.springframework.web.client.RestClient;

public class ChannelConnectionChecker implements ChannelConnectionCheckPort {

    public ChannelConnectionChecker(RestClient restClient, ObjectMapper objectMapper, String graphBaseUrl,
                                    String graphVersion, String geminiBaseUrl) {
    }

    @Override
    public CheckResult checkWhatsApp(String phoneNumberId, String accessToken) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public CheckResult checkGemini(String apiKey, String model) {
        throw new UnsupportedOperationException("pendiente");
    }
}
