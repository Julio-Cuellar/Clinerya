package com.jclinical.automation.domain.ports.out;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Avisos en tiempo real a las pantallas abiertas de Clinerya. Solo senales (que cambio y donde), nunca
 * contenido de chats: leer un chat se hace por el endpoint auditado. Se entregan despues del commit.
 */
public interface RealtimeNotifierPort {

    void chatActivity(UUID clinicId, String phone, LocalDateTime at);

    void newAppointmentRequest(UUID clinicId, UUID doctorStaffId, UUID requestId);
}
