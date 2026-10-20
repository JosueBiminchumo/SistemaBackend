package com.mediturno.backend.auth.security;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

final class MutableClock extends Clock {

    private final AtomicReference<Instant> instant;
    private final ZoneId zone;

    MutableClock(Instant initialInstant) {
        this(initialInstant, ZoneId.of("UTC"));
    }

    private MutableClock(Instant initialInstant, ZoneId zone) {
        this.instant = new AtomicReference<>(initialInstant);
        this.zone = zone;
    }

    void advanceSeconds(long seconds) {
        instant.updateAndGet(current -> current.plusSeconds(seconds));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instant(), zone);
    }

    @Override
    public Instant instant() {
        return instant.get();
    }
}
