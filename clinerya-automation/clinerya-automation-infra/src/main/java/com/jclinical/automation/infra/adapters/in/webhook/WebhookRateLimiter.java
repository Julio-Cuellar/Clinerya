package com.jclinical.automation.infra.adapters.in.webhook;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Tope de peticiones por llave de webhook en ventanas de un minuto. Como la llave viene de una URL
 * publica, el mapa se purga cuando crece demasiado para que llaves inventadas no agoten memoria.
 */
public class WebhookRateLimiter {

    static final long WINDOW_MILLIS = 60_000L;
    static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxRequestsPerMinute;
    private final LongSupplier nowMillis;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public WebhookRateLimiter(int maxRequestsPerMinute, LongSupplier nowMillis) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.nowMillis = nowMillis;
    }

    public boolean tryAcquire(String webhookKey) {
        long now = nowMillis.getAsLong();
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().start() >= WINDOW_MILLIS);
        }
        Window window = windows.compute(webhookKey == null ? "" : webhookKey, (key, current) ->
                current == null || now - current.start() >= WINDOW_MILLIS
                        ? new Window(now, 1)
                        : new Window(current.start(), current.count() + 1));
        return window.count() <= maxRequestsPerMinute;
    }

    private record Window(long start, int count) {}
}
