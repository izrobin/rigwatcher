package com.robinjonsson.rigwatcher;

import java.time.Instant;

public class HealthResult {

    private final Instant lastSeen;
    private final boolean ok;
    private final String message;

    public HealthResult(
        final Instant lastSeen,
        final boolean ok,
        final String message
    ) {
        this.lastSeen = lastSeen;
        this.ok = ok;
        this.message = message;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "HealthResult{" +
            "lastSeen=" + lastSeen +
            ", ok=" + ok +
            ", message='" + message + '\'' +
            '}';
    }
}
