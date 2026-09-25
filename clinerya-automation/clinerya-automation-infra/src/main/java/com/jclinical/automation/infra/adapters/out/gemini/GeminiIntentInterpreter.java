package com.jclinical.automation.infra.adapters.out.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.ports.out.IntentInterpreterPort;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

public class GeminiIntentInterpreter implements IntentInterpreterPort {

    public GeminiIntentInterpreter(RestClient restClient, ObjectMapper objectMapper, String baseUrl, String model, String apiKey) {
    }

    @Override
    public Optional<String> interpret(String text, List<ConversationOption> options) {
        return Optional.empty();
    }
}
