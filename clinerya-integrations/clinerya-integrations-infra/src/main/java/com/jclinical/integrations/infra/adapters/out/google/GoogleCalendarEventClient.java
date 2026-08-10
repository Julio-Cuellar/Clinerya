package com.jclinical.integrations.infra.adapters.out.google;

import com.jclinical.integrations.domain.model.CalendarCredentials;
import com.jclinical.integrations.domain.model.CalendarEventDraft;
import com.jclinical.integrations.domain.ports.out.CalendarEventPort;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GoogleCalendarEventClient implements CalendarEventPort {

    private static final String CALENDAR_API_BASE = "https://www.googleapis.com/calendar/v3/calendars";

    private final String defaultTimeZone;
    private final WebClient webClient;

    public GoogleCalendarEventClient(@Value("${google.calendar.default-timezone:America/Mexico_City}") String defaultTimeZone) {
        this.defaultTimeZone = defaultTimeZone;
        ConnectionProvider connectionProvider = ConnectionProvider.builder("google-calendar")
                .maxConnections(20)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofMinutes(2))
                .evictInBackground(Duration.ofSeconds(30))
                .build();
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(20));
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public String createEvent(CalendarCredentials credentials, CalendarEventDraft draft) {
        Map<String, Object> response = webClient.post()
                .uri(eventsUrl(credentials))
                .headers(headers -> headers.setBearerAuth(credentials.getAccessToken()))
                .bodyValue(toEventBody(draft))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return response == null ? null : (String) response.get("id");
    }

    @Override
    public void updateEvent(CalendarCredentials credentials, String externalEventId, CalendarEventDraft draft) {
        webClient.patch()
                .uri(eventsUrl(credentials) + "/" + externalEventId)
                .headers(headers -> headers.setBearerAuth(credentials.getAccessToken()))
                .bodyValue(toEventBody(draft))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @Override
    public void deleteEvent(CalendarCredentials credentials, String externalEventId) {
        try {
            webClient.delete()
                    .uri(eventsUrl(credentials) + "/" + externalEventId)
                    .headers(headers -> headers.setBearerAuth(credentials.getAccessToken()))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404 || e.getStatusCode().value() == 410) {
                log.info("El evento {} ya no existe en Google Calendar, se ignora.", externalEventId);
                return;
            }
            throw e;
        }
    }

    @Override
    public EventsPage listEvents(CalendarCredentials credentials, String syncToken) {
        boolean useSyncToken = syncToken != null && !syncToken.trim().isEmpty();
        List<ExternalEventSnapshot> allEvents = new ArrayList<>();
        String pageToken = null;
        String nextSyncToken = null;

        do {
            org.springframework.web.util.UriComponentsBuilder uriBuilder = org.springframework.web.util.UriComponentsBuilder
                    .fromUriString(eventsUrl(credentials));

            if (useSyncToken) {
                uriBuilder.queryParam("syncToken", syncToken);
            } else if (!credentials.isImportPastEvents()) {
                // Sin syncToken y sin pedir historial: solo trae desde ayer en adelante.
                ZonedDateTime nowMinusOneDay = ZonedDateTime.now(ZoneId.of(defaultTimeZone)).minusDays(1);
                uriBuilder.queryParam("timeMin", nowMinusOneDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
            // Si importPastEvents=true y no hay syncToken todavía: no se manda timeMin,
            // así Google devuelve el historial completo del calendario.

            if (pageToken != null) {
                uriBuilder.queryParam("pageToken", pageToken);
            }

            Map<String, Object> response;
            try {
                response = webClient.get()
                        .uri(uriBuilder.build().toUri())
                        .headers(headers -> headers.setBearerAuth(credentials.getAccessToken()))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .retryWhen(transientNetworkRetry())
                        .block();
            } catch (WebClientResponseException e) {
                if (e.getStatusCode().value() == 410) {
                    log.info("Google Calendar syncToken expirado para la cuenta: {}", credentials.getGoogleAccountEmail());
                    return new EventsPage(List.of(), null, true);
                }
                throw e;
            }

            if (response == null) {
                break;
            }

            Object syncTokenValue = response.get("nextSyncToken");
            if (syncTokenValue != null) {
                nextSyncToken = (String) syncTokenValue;
            }
            pageToken = (String) response.get("nextPageToken");

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    try {
                        allEvents.add(parseSnapshot(item));
                    } catch (Exception e) {
                        log.warn("Error parseando evento individual de Google Calendar, se salta. Evento: {}", item, e);
                    }
                }
            }
        } while (pageToken != null);

        return new EventsPage(allEvents, nextSyncToken, false);
    }

    private Retry transientNetworkRetry() {
        return Retry.backoff(2, Duration.ofMillis(500))
                .maxBackoff(Duration.ofSeconds(3))
                .filter(this::isTransientNetworkFailure)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    private boolean isTransientNetworkFailure(Throwable throwable) {
        return throwable instanceof WebClientRequestException
                || throwable instanceof java.net.SocketException
                || throwable instanceof java.util.concurrent.TimeoutException;
    }

    private ExternalEventSnapshot parseSnapshot(Map<String, Object> item) {
        String id = (String) item.get("id");
        String status = (String) item.get("status");
        String summary = (String) item.get("summary");
        String description = (String) item.get("description");

        Map<String, Object> startMap = (Map<String, Object>) item.get("start");
        Map<String, Object> endMap = (Map<String, Object>) item.get("end");

        LocalDateTime start = parseGoogleTime(startMap);
        LocalDateTime end = parseGoogleTime(endMap);
        if (!end.toLocalDate().equals(start.toLocalDate())) {
            // Google reporta el fin de eventos de todo el día como fecha EXCLUSIVA (ej. un
            // bloqueo de un solo martes viene con start=martes, end=miércoles), y también
            // puede haber eventos con hora que cruzan la medianoche (ej. 11pm-1am). En
            // ambos casos lo acotamos al mismo día de inicio, porque el dominio no permite
            // que una cita cruce la medianoche entre dos días distintos.
            end = start.toLocalDate().atTime(23, 59, 59);
        }

        boolean cancelled = "cancelled".equalsIgnoreCase(status);

        boolean createdByJClinical = false;
        Map<String, Object> extProps = (Map<String, Object>) item.get("extendedProperties");
        if (extProps != null) {
            Map<String, Object> privProps = (Map<String, Object>) extProps.get("private");
            if (privProps != null) {
                createdByJClinical = privProps.containsKey("jclinicalAppointmentId");
            }
        }

        return new ExternalEventSnapshot(id, summary, description, start, end, cancelled, createdByJClinical);
    }

    private LocalDateTime parseGoogleTime(Map<String, Object> timeMap) {
        if (timeMap == null) {
            return LocalDateTime.now();
        }
        String dateTimeStr = (String) timeMap.get("dateTime");
        if (dateTimeStr != null) {
            ZonedDateTime zdt = ZonedDateTime.parse(dateTimeStr);
            return zdt.withZoneSameInstant(ZoneId.of(defaultTimeZone)).toLocalDateTime();
        }
        String dateStr = (String) timeMap.get("date");
        if (dateStr != null) {
            return LocalDate.parse(dateStr).atStartOfDay();
        }
        return LocalDateTime.now();
    }

    private String eventsUrl(CalendarCredentials credentials) {
        return CALENDAR_API_BASE + "/" + credentials.getGoogleCalendarId() + "/events";
    }

    private Map<String, Object> toEventBody(CalendarEventDraft draft) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        Map<String, Object> start = Map.of("dateTime", draft.start().format(formatter), "timeZone", defaultTimeZone);
        Map<String, Object> end = Map.of("dateTime", draft.end().format(formatter), "timeZone", defaultTimeZone);

        Map<String, Object> body = new HashMap<>();
        body.put("summary", draft.summary() == null ? "" : draft.summary());
        body.put("description", draft.description() == null ? "" : draft.description());
        body.put("start", start);
        body.put("end", end);

        if (draft.appointmentId() != null) {
            body.put("extendedProperties", Map.of(
                    "private", Map.of("jclinicalAppointmentId", draft.appointmentId().toString())
            ));
        }

        return body;
    }
}
