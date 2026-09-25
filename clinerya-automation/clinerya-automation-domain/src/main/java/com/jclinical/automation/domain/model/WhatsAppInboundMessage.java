package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;

/**
 * Un mensaje tal como lo entrega Meta, ya leido del JSON. {@code phoneNumberId} es el numero de la
 * clinica que lo recibio; {@code fromPhone}, el del paciente.
 */
public record WhatsAppInboundMessage(
        String phoneNumberId,
        String waMessageId,
        String fromPhone,
        Kind kind,
        String text,
        String selectedOptionId,
        LocalDateTime sentAt
) {

    public enum Kind {
        /** Texto libre. */
        TEXT,
        /** Boton o fila de lista: trae el id de la opcion elegida. */
        OPTION,
        /** Audio, imagen, ubicacion, etc.: por ahora no se lee. */
        UNSUPPORTED
    }
}
