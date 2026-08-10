package com.jclinical.integrations.domain.ports.out;

import java.time.LocalDateTime;

public interface GoogleOAuthPort {

    String buildAuthorizationUrl(String state);

    GoogleTokenResponse exchangeAuthorizationCode(String code);

    GoogleTokenResponse refreshAccessToken(String refreshToken);

    String fetchAccountEmail(String accessToken);

    void revokeToken(String token);

    record GoogleTokenResponse(String accessToken, String refreshToken, LocalDateTime expiry) {}
}
