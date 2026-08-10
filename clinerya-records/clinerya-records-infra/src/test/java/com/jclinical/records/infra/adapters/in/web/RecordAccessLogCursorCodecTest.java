package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.ports.in.ManageRecordAccessLogUseCase.AccessLogCursor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordAccessLogCursorCodecTest {

    @Test
    void shouldRoundTripOpaqueCursor() {
        AccessLogCursor cursor = new AccessLogCursor(
                LocalDateTime.of(2026, 7, 16, 11, 30, 15, 123456000),
                UUID.randomUUID());

        String encoded = RecordAccessLogCursorCodec.encode(cursor);

        assertThat(RecordAccessLogCursorCodec.decode(encoded)).isEqualTo(cursor);
    }

    @Test
    void shouldTreatMissingCursorAsFirstPage() {
        assertThat(RecordAccessLogCursorCodec.decode(null)).isNull();
        assertThat(RecordAccessLogCursorCodec.decode(" ")).isNull();
    }

    @Test
    void shouldRejectMalformedCursor() {
        assertThatThrownBy(() -> RecordAccessLogCursorCodec.decode("not-a-cursor"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cursor");
    }
}
