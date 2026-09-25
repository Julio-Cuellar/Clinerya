package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;

/** Aviso de Meta sobre un mensaje que la clinica envio: enviado, entregado, leido o fallido. */
public record DeliveryStatusUpdate(
        String phoneNumberId,
        String waMessageId,
        Status status,
        LocalDateTime at,
        String errorCode
) {

    public enum Status {
        SENT,
        DELIVERED,
        READ,
        FAILED
    }
}
