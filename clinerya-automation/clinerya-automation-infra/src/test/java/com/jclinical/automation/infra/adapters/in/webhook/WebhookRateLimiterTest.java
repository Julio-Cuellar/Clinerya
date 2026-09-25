package com.jclinical.automation.infra.adapters.in.webhook;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La ruta publica del webhook no acepta rafagas sin limite: tope por llave y por minuto. */
class WebhookRateLimiterTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);
    private final WebhookRateLimiter limiter = new WebhookRateLimiter(3, now::get);

    @Test
    void acceptsUpToTheLimitWithinAMinute() {
        assertTrue(limiter.tryAcquire("llave"));
        assertTrue(limiter.tryAcquire("llave"));
        assertTrue(limiter.tryAcquire("llave"));

        assertFalse(limiter.tryAcquire("llave"));
    }

    @Test
    void eachWebhookHasItsOwnLimit() {
        limiter.tryAcquire("a");
        limiter.tryAcquire("a");
        limiter.tryAcquire("a");

        assertTrue(limiter.tryAcquire("b"));
    }

    @Test
    void theLimitResetsTheNextMinute() {
        limiter.tryAcquire("llave");
        limiter.tryAcquire("llave");
        limiter.tryAcquire("llave");

        now.addAndGet(60_000L);

        assertTrue(limiter.tryAcquire("llave"));
    }
}
