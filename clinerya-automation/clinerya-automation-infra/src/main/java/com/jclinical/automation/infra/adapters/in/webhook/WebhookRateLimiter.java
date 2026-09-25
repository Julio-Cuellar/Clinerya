package com.jclinical.automation.infra.adapters.in.webhook;

import java.util.function.LongSupplier;

public class WebhookRateLimiter {

    public WebhookRateLimiter(int maxRequestsPerMinute, LongSupplier nowMillis) {
    }

    public boolean tryAcquire(String webhookKey) {
        throw new UnsupportedOperationException("pendiente");
    }
}
