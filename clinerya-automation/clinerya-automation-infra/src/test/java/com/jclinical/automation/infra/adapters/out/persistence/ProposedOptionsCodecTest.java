package com.jclinical.automation.infra.adapters.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jclinical.automation.domain.model.AppointmentRequest.ProposedOption;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las opciones que propuso el medico (con su apartado) sobreviven el guardado. */
class ProposedOptionsCodecTest {

    private final ProposedOptionsCodec codec = new ProposedOptionsCodec(new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

    @Test
    void proposedOptionsRoundTrip() {
        List<ProposedOption> options = List.of(
                new ProposedOption(UUID.randomUUID(), LocalDateTime.of(2026, 9, 30, 10, 0), LocalDateTime.of(2026, 9, 30, 10, 30)),
                new ProposedOption(UUID.randomUUID(), LocalDateTime.of(2026, 10, 1, 12, 0), LocalDateTime.of(2026, 10, 1, 12, 30)));

        String json = codec.encode(options);

        assertTrue(json.contains("2026-09-30T10:00"), json);
        assertEquals(options, codec.decode(json));
    }

    @Test
    void nothingStoredMeansNoOptions() {
        assertEquals(List.of(), codec.decode(null));
        assertEquals(List.of(), codec.decode(" "));
        assertEquals("[]", codec.encode(List.of()));
    }
}
