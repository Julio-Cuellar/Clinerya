package com.jclinical.records.infra.audit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RecordAccessLogMetrics {

    private final Counter enqueued;
    private final Counter dispatched;
    private final Counter dispatchFailures;
    private final Timer dispatchDuration;
    private final AtomicLong pending = new AtomicLong();

    public RecordAccessLogMetrics(MeterRegistry registry) {
        this.enqueued = registry.counter("medicloud.audit.outbox.enqueued");
        this.dispatched = registry.counter("medicloud.audit.outbox.dispatched");
        this.dispatchFailures = registry.counter("medicloud.audit.outbox.dispatch.failures");
        this.dispatchDuration = registry.timer("medicloud.audit.outbox.dispatch.duration");
        Gauge.builder("medicloud.audit.outbox.pending", pending, AtomicLong::get)
                .description("Access audit events waiting to be persisted")
                .register(registry);
    }

    public void recordEnqueued() {
        enqueued.increment();
    }

    public void recordDispatch(int count, Duration duration) {
        dispatched.increment(count);
        dispatchDuration.record(duration);
    }

    public void recordDispatchFailure(Duration duration) {
        dispatchFailures.increment();
        dispatchDuration.record(duration);
    }

    public void updatePending(long count) {
        pending.set(count);
    }
}
