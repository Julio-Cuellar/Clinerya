package com.jclinical.automation.domain.ports.out;

import java.time.LocalDateTime;
import java.util.UUID;

/** Registro de ids de WhatsApp ya recibidos: Meta reintenta y el mismo mensaje puede llegar dos veces. */
public interface InboundMessageLedgerPort {

    /**
     * Registra el mensaje y su remitente (el remitente abre la ventana de 24 h de WhatsApp).
     *
     * @return true si es la primera vez que se ve ese id.
     */
    boolean recordIfNew(UUID clinicId, String waMessageId, String fromPhone, LocalDateTime receivedAt);
}
