package com.jclinical.integrations.domain.service;

import com.jclinical.integrations.domain.model.CalendarCredentials;
import com.jclinical.integrations.domain.ports.in.ManageCalendarIntegrationUseCase;
import com.jclinical.integrations.domain.ports.out.CalendarCredentialsRepositoryPort;
import com.jclinical.integrations.domain.ports.out.GoogleOAuthPort;
import com.jclinical.integrations.domain.ports.out.GoogleOAuthPort.GoogleTokenResponse;
import com.jclinical.integrations.domain.ports.out.StateCodecPort;
import com.jclinical.integrations.domain.ports.out.StateCodecPort.DecodedState;

import java.time.LocalDateTime;
import java.util.UUID;

public class CalendarIntegrationService implements ManageCalendarIntegrationUseCase {

    private final CalendarCredentialsRepositoryPort credentialsRepository;
    private final GoogleOAuthPort oAuthPort;
    private final StateCodecPort stateCodec;

    public CalendarIntegrationService(
            CalendarCredentialsRepositoryPort credentialsRepository,
            GoogleOAuthPort oAuthPort,
            StateCodecPort stateCodec) {
        this.credentialsRepository = credentialsRepository;
        this.oAuthPort = oAuthPort;
        this.stateCodec = stateCodec;
    }

    @Override
    public String getAuthorizationUrl(UUID clinicId, UUID staffId, boolean importPastEvents) {
        String state = stateCodec.encode(clinicId, staffId, importPastEvents);
        return oAuthPort.buildAuthorizationUrl(state);
    }

    @Override
    public void handleOAuthCallback(String state, String code) {
        DecodedState decoded = stateCodec.decode(state);
        GoogleTokenResponse tokenResponse = oAuthPort.exchangeAuthorizationCode(code);
        String email = oAuthPort.fetchAccountEmail(tokenResponse.accessToken());

        CalendarCredentials credentials = credentialsRepository
                .findByClinicIdAndStaffId(decoded.clinicId(), decoded.staffId())
                .orElseGet(() -> CalendarCredentials.builder()
                        .id(UUID.randomUUID())
                        .clinicId(decoded.clinicId())
                        .staffId(decoded.staffId())
                        .googleCalendarId("primary")
                        .createdAt(LocalDateTime.now())
                        .build());

        String refreshToken = tokenResponse.refreshToken() != null
                ? tokenResponse.refreshToken()
                : credentials.getRefreshToken();
        if (refreshToken == null) {
            throw new IllegalStateException(
                    "Google no otorgó un refresh_token. Vuelve a intentar la conexión autorizando el acceso offline.");
        }

        credentials.setGoogleAccountEmail(email);
        credentials.setAccessToken(tokenResponse.accessToken());
        credentials.setRefreshToken(refreshToken);
        credentials.setTokenExpiry(tokenResponse.expiry());
        credentials.setImportPastEvents(decoded.importPastEvents());
        credentials.setUpdatedAt(LocalDateTime.now());

        credentialsRepository.save(credentials);
    }


    @Override
    public void disconnect(UUID clinicId, UUID staffId) {
        credentialsRepository.findByClinicIdAndStaffId(clinicId, staffId).ifPresent(credentials -> {
            oAuthPort.revokeToken(credentials.getRefreshToken());
            credentialsRepository.deleteByClinicIdAndStaffId(clinicId, staffId);
        });
    }

    @Override
    public CalendarConnectionStatus getStatus(UUID clinicId, UUID staffId) {
        return credentialsRepository.findByClinicIdAndStaffId(clinicId, staffId)
                .map(credentials -> new CalendarConnectionStatus(true, credentials.getGoogleAccountEmail(), credentials.isImportPastEvents()))
                .orElse(new CalendarConnectionStatus(false, null, false));
    }

    @Override
    public void updatePreferences(UUID clinicId, UUID staffId, boolean importPastEvents) {
        credentialsRepository.findByClinicIdAndStaffId(clinicId, staffId).ifPresent(credentials -> {
            if (credentials.isImportPastEvents() != importPastEvents) {
                credentials.setImportPastEvents(importPastEvents);
                credentials.setCalendarSyncToken(null); // Limpiar syncToken para forzar una sincronización completa
                credentials.setUpdatedAt(LocalDateTime.now());
                credentialsRepository.save(credentials);
            }
        });
    }

}

