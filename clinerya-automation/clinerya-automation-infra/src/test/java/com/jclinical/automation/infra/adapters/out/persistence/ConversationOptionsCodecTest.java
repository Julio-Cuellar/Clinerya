package com.jclinical.automation.infra.adapters.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.ConversationOption;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las opciones ofrecidas son la lista blanca de la conversacion: si se pierden o se alteran al
 * guardar, el siguiente mensaje del paciente se validaria contra otra cosa.
 */
class ConversationOptionsCodecTest {

    private final ConversationOptionsCodec codec = new ConversationOptionsCodec(new ObjectMapper());

    @Test
    void theOfferedOptionsSurviveARoundTrip() {
        List<ConversationOption> options = List.of(
                new ConversationOption("slot:2026-09-25T10:00|2026-09-25T10:45", "Jue 25/09 10:00"),
                new ConversationOption("action:show-doctors", "Ver médicos de la clínica"));

        assertEquals(options, codec.decode(codec.encode(options)));
    }

    @Test
    void noOptionsIsAnEmptyList() {
        assertTrue(codec.decode(codec.encode(List.of())).isEmpty());
        assertTrue(codec.decode(null).isEmpty());
    }
}
