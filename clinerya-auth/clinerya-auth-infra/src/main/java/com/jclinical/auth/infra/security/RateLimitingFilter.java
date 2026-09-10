package com.jclinical.auth.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita por IP las peticiones a endpoints públicos sensibles a fuerza bruta o abuso.
 * El bloqueo de cuenta tras varios intentos fallidos (ver {@code User.registerFailedLoginAttempt})
 * protege una cuenta puntual; este filtro protege contra ataques distribuidos entre muchas
 * cuentas, la enumeración de tokens y el envío masivo de correos desde una misma IP.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private record Limit(int maxRequests, long windowMillis) {
    }

    private static final long FIFTEEN_MINUTES = 15 * 60_000L;

    private static final Map<String, Limit> LIMITS_BY_PATH = Map.of(
            "/api/v1/auth/login", new Limit(10, 60_000L),
            "/api/v1/users/password-reset/request", new Limit(5, FIFTEEN_MINUTES),
            "/api/v1/users/password-reset/confirm", new Limit(10, FIFTEEN_MINUTES),
            // Alta masiva de cuentas y bombardeo de correo desde una sola IP.
            "/api/v1/users/register", new Limit(5, FIFTEEN_MINUTES),
            "/api/v1/users/resend-verification", new Limit(5, FIFTEEN_MINUTES),
            // El código de verificación es de 6 caracteres y se busca sin acoplarse al correo.
            "/api/v1/users/verify-email", new Limit(10, FIFTEEN_MINUTES),
            // Enumeración del token de invitación (12 caracteres).
            "/api/v1/users/register-staff", new Limit(10, FIFTEEN_MINUTES),
            // Endpoint público que devuelve expediente completo a quien tenga el token.
            "/api/v1/public/shared-history", new Limit(30, FIFTEEN_MINUTES)
    );

    // Descarga de estudios por enlace compartido: la ruta lleva el id del adjunto,
    // así que se limita por prefijo (todas las descargas comparten bucket por IP).
    private static final String SHARED_STUDY_PREFIX = "/api/v1/public/shared-history/studies/";
    private static final Limit SHARED_STUDY_LIMIT = new Limit(60, FIFTEEN_MINUTES);

    private static final long STALE_BUCKET_MILLIS = 30 * 60_000L;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Cuántos proxies de confianza hay delante de la aplicación. Cada uno añade su salto al
     * final de {@code X-Forwarded-For} (nginx con {@code $proxy_add_x_forwarded_for}, Caddy
     * con {@code reverse_proxy}), así que la IP real es la n-ésima contando desde la derecha.
     * Con 0 se ignora la cabecera por completo.
     */
    private final int trustedProxyCount;

    public RateLimitingFilter(@Value("${medicloud.security.trusted-proxy-count:1}") int trustedProxyCount) {
        this.trustedProxyCount = trustedProxyCount;
    }

    private static final class Bucket {
        volatile long windowStart = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        Limit limit = LIMITS_BY_PATH.get(uri);
        String limitKey = uri;
        if (limit == null && uri.startsWith(SHARED_STUDY_PREFIX)) {
            limit = SHARED_STUDY_LIMIT;
            limitKey = SHARED_STUDY_PREFIX;
        }
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = clientIp(request);
        String key = clientIp + "|" + limitKey;

        if (isOverLimit(key, limit)) {
            log.warn("Límite de peticiones excedido: IP='{}', endpoint='{}'", clientIp, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(limit.windowMillis() / 1000));
            response.getWriter().write("{\"message\":\"Demasiadas solicitudes. Intenta de nuevo más tarde.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isOverLimit(String key, Limit limit) {
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());

        synchronized (bucket) {
            if (now - bucket.windowStart > limit.windowMillis()) {
                bucket.windowStart = now;
                bucket.count.set(0);
            }
            return bucket.count.incrementAndGet() > limit.maxRequests();
        }
    }

    /**
     * Toma el salto que añadió nuestro proxy de confianza, contando desde la derecha.
     *
     * <p>Tomar el primer valor de {@code X-Forwarded-For} —como se hacía antes— deja el
     * límite sin efecto: esa entrada la escribe el cliente, así que basta con mandar una IP
     * distinta en cada intento para estrenar bucket. Las entradas de la derecha las escriben
     * los proxies y el cliente no puede falsificarlas.
     */
    String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (trustedProxyCount > 0 && forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            int index = hops.length - trustedProxyCount;
            if (index >= 0 && index < hops.length) {
                String hop = hops[index].trim();
                if (!hop.isEmpty()) {
                    return hop;
                }
            }
            // Menos saltos de los esperados (alguien llegó saltándose el proxy, o la
            // configuración no corresponde): se usa el peer directo en vez de confiar
            // en un valor que pudo escribir el cliente.
        }
        return request.getRemoteAddr();
    }

    @Scheduled(fixedRate = 10 * 60_000L)
    void cleanupStaleBuckets() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> now - entry.getValue().windowStart > STALE_BUCKET_MILLIS);
    }
}
