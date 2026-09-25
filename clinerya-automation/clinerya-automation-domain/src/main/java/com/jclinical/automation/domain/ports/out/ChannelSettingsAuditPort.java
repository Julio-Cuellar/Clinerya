package com.jclinical.automation.domain.ports.out;

import java.time.LocalDateTime;
import java.util.UUID;

/** Bitacora de cambios a la configuracion del asistente. Nunca recibe valores de secretos. */
public interface ChannelSettingsAuditPort {

    void record(UUID clinicId, UUID userId, String action, LocalDateTime at);
}
