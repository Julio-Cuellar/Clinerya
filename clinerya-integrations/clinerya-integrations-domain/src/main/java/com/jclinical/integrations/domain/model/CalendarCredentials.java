package com.jclinical.integrations.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarCredentials {
    private UUID id;
    private UUID clinicId;
    private UUID staffId;
    private String googleAccountEmail;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpiry;
    private String googleCalendarId;
    private String calendarSyncToken;
    private boolean importPastEvents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public boolean isAccessTokenExpiringSoon() {
        return tokenExpiry == null || tokenExpiry.isBefore(LocalDateTime.now().plusMinutes(2));
    }

    public void updateAccessToken(String newAccessToken, LocalDateTime newExpiry) {
        this.accessToken = newAccessToken;
        this.tokenExpiry = newExpiry;
        this.updatedAt = LocalDateTime.now();
    }
}
