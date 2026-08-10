package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.ports.in.ManageRecordAccessLogUseCase.AccessLogCursor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

final class RecordAccessLogCursorCodec {

    private RecordAccessLogCursorCodec() {
    }

    static String encode(AccessLogCursor cursor) {
        String value = cursor.createdAt() + "|" + cursor.id();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static AccessLogCursor decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        if (encoded.length() > 256) {
            throw invalidCursor();
        }
        try {
            String value = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = value.split("\\|", -1);
            if (parts.length != 2) {
                throw invalidCursor();
            }
            return new AccessLogCursor(LocalDateTime.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw invalidCursor();
        }
    }

    private static IllegalArgumentException invalidCursor() {
        return new IllegalArgumentException("El cursor de la bitacora no es valido.");
    }
}
