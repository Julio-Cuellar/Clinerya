package com.jclinical.integrations.infra.adapters.out.persistence;

import com.jclinical.integrations.infra.adapters.out.persistence.security.AesCryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "staff_calendar_credentials", schema = "integrations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCalendarCredentialsEntity {

    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "google_account_email", nullable = false)
    private String googleAccountEmail;

    @Column(name = "access_token_enc", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = AesCryptoConverter.class)
    private String accessToken;

    @Column(name = "refresh_token_enc", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = AesCryptoConverter.class)
    private String refreshToken;

    @Column(name = "token_expiry", nullable = false)
    private LocalDateTime tokenExpiry;

    @Column(name = "google_calendar_id", nullable = false)
    private String googleCalendarId;

    @Column(name = "calendar_sync_token")
    private String calendarSyncToken;

    @Column(name = "import_past_events", nullable = false)
    @Builder.Default
    private boolean importPastEvents = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

