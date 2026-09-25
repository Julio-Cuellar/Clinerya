package com.jclinical.automation.domain.ports.out;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Ultimo mensaje que ese celular le escribio a la clinica (abre la ventana de 24 h de WhatsApp). */
public interface CustomerServiceWindowPort {

    Optional<LocalDateTime> lastInboundAt(UUID clinicId, String phone);
}
