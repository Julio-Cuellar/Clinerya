package com.jclinical.auth.infra.adapters.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "revoked_tokens", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevokedTokenEntity {

    /** SHA-256 del token en hexadecimal: nunca se guarda el token en claro. */
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private LocalDateTime revokedAt;
}
