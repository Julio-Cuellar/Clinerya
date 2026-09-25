package com.jclinical.agenda.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cupo apartado mientras el medico decide una solicitud de cita. Mientras este ACTIVE y no haya
 * vencido, nadie puede agendar encima: ni otro paciente por WhatsApp ni el personal desde la agenda.
 * {@code reference} es el id de la solicitud que lo aparto.
 */
public record SlotHold(
        UUID id,
        UUID clinicId,
        UUID doctorStaffId,
        LocalDateTime start,
        LocalDateTime end,
        LocalDateTime expiresAt,
        UUID reference,
        Status status,
        LocalDateTime createdAt
) {

    public enum Status {
        ACTIVE,
        /** Liberado antes de vencer (el medico rechazo o el paciente desistio). */
        RELEASED,
        /** Convertido en cita al aprobarse la solicitud. */
        CONSUMED
    }

    public boolean isActiveAt(LocalDateTime now) {
        return status == Status.ACTIVE && expiresAt.isAfter(now);
    }

    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }

    public SlotHold withStatus(Status newStatus) {
        return new SlotHold(id, clinicId, doctorStaffId, start, end, expiresAt, reference, newStatus, createdAt);
    }
}
