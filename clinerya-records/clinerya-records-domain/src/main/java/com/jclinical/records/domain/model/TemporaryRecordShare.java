package com.jclinical.records.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryRecordShare {
    private UUID id;
    private UUID clinicId;
    private UUID patientId;
    private String email;
    /** SHA-256 (base64url) del token. Lo unico que se persiste. */
    private String tokenHash;
    private UUID createdByUserId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;
    private LocalDateTime recipientVerifiedAt;
    private LocalDateTime lastAccessedAt;
    private int accessCount;

    /** Secciones del expediente que este enlace expone. */
    private Set<SharedSection> sharedSections;

    /**
     * Token en claro. Solo se llena al crear el enlace, para devolverlo una vez
     * al emisor. Nunca se persiste ni se vuelve a exponer.
     */
    private transient String plaintextToken;

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsable() {
        return !isRevoked() && !isExpired();
    }

    public void registerAccess(LocalDateTime when) {
        this.lastAccessedAt = when;
        this.accessCount += 1;
    }

    /** Un enlace sin secciones definidas expone todo (compatibilidad). */
    public boolean includes(SharedSection section) {
        return sharedSections == null || sharedSections.isEmpty() || sharedSections.contains(section);
    }
}
