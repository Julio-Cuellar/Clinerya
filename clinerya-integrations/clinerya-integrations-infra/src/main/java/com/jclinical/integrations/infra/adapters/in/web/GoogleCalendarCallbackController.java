package com.jclinical.integrations.infra.adapters.in.web;

import com.jclinical.integrations.domain.ports.in.ManageCalendarIntegrationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * URL fija registrada como redirect_uri en Google Cloud Console (Google no permite
 * variables de ruta en el redirect_uri). El clinicId/staffId viajan dentro del
 * parámetro "state" firmado, no en la ruta. Debe quedar permitAll en SecurityConfig
 * porque Google llega aquí sin el JWT de la sesión de la app.
 */
@RestController
@RequestMapping("/api/v1/integrations/google-calendar")
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarCallbackController {

    private final ManageCalendarIntegrationUseCase calendarIntegrationUseCase;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        String redirectStatus = "error";
        if (error == null && code != null && state != null) {
            try {
                calendarIntegrationUseCase.handleOAuthCallback(state, code);
                redirectStatus = "connected";
            } catch (Exception e) {
                log.warn("Fallo al procesar el callback de Google Calendar", e);
            }
        }

        URI redirectUri = UriComponentsBuilder.fromHttpUrl(frontendBaseUrl)
                .queryParam("googleCalendar", redirectStatus)
                .build()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
    }
}
