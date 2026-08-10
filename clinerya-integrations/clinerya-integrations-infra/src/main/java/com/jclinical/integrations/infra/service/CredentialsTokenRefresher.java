package com.jclinical.integrations.infra.service;

import com.jclinical.integrations.domain.model.CalendarCredentials;
import com.jclinical.integrations.domain.ports.out.CalendarCredentialsRepositoryPort;
import com.jclinical.integrations.domain.ports.out.GoogleOAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CredentialsTokenRefresher {

    private final CalendarCredentialsRepositoryPort credentialsRepository;
    private final GoogleOAuthPort oAuthPort;

    public CalendarCredentials ensureFreshToken(CalendarCredentials credentials) {
        if (!credentials.isAccessTokenExpiringSoon()) {
            return credentials;
        }
        GoogleOAuthPort.GoogleTokenResponse refreshed = oAuthPort.refreshAccessToken(credentials.getRefreshToken());
        credentials.updateAccessToken(refreshed.accessToken(), refreshed.expiry());
        return credentialsRepository.save(credentials);
    }
}
