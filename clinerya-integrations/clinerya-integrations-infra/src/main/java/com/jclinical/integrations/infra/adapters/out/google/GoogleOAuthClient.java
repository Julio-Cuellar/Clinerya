package com.jclinical.integrations.infra.adapters.out.google;

import com.jclinical.integrations.domain.ports.out.GoogleOAuthPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@Slf4j
public class GoogleOAuthClient implements GoogleOAuthPort {

    private static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String SCOPES = "https://www.googleapis.com/auth/calendar.events email";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final WebClient webClient;

    public GoogleOAuthClient(
            @Value("${google.oauth.client-id:}") String clientId,
            @Value("${google.oauth.client-secret:}") String clientSecret,
            @Value("${google.oauth.redirect-uri}") String redirectUri) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException(
                    "google.oauth.client-id / google.oauth.client-secret no están configurados. "
                            + "Define las variables de entorno GOOGLE_OAUTH_CLIENT_ID y GOOGLE_OAUTH_CLIENT_SECRET "
                            + "antes de arrancar la aplicación.");
        }
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.webClient = WebClient.builder().build();
    }

    @Override
    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZATION_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPES)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    @Override
    public GoogleTokenResponse exchangeAuthorizationCode(String code) {
        Map<String, Object> response = webClient.post()
                .uri(TOKEN_URL)
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=authorization_code"
                        + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                        + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                        + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return toTokenResponse(response);
    }

    @Override
    public GoogleTokenResponse refreshAccessToken(String refreshToken) {
        Map<String, Object> response = webClient.post()
                .uri(TOKEN_URL)
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=refresh_token"
                        + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                        + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                        + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return toTokenResponse(response);
    }

    @Override
    public String fetchAccountEmail(String accessToken) {
        Map<String, Object> response = webClient.get()
                .uri(USERINFO_URL)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return response == null ? null : (String) response.get("email");
    }

    @Override
    public void revokeToken(String token) {
        try {
            webClient.post()
                    .uri(REVOKE_URL + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8))
                    .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("No se pudo revocar el token en Google (puede que ya haya sido revocado)", e);
        }
    }

    @SuppressWarnings("unchecked")
    private GoogleTokenResponse toTokenResponse(Map<String, Object> response) {
        if (response == null) {
            throw new IllegalStateException("Google no devolvió una respuesta válida al intercambiar el token.");
        }
        String accessToken = (String) response.get("access_token");
        String refreshToken = (String) response.get("refresh_token");
        Number expiresIn = (Number) response.get("expires_in");
        LocalDateTime expiry = LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn.longValue() : 3600L);
        return new GoogleTokenResponse(accessToken, refreshToken, expiry);
    }
}
