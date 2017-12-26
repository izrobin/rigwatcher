package com.robinjonsson.rigwatcher;

import java.time.Instant;

public class HealthResult {

    private final Instant lastSeen;
    private final Health health;
    private final String message;

    public HealthResult(
        final Instant lastSeen,
        final Health health,
        final String message
    ) {
        this.lastSeen = lastSeen;
        this.health = health;
        this.message = message;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public Health getHealth() {
        return health;
    }

    public String getMessage() {
        return message;
    }

    public enum Health {
        API_ERROR,
        MISSING_IN_ACTION,
        HIGH_STALES,
        LOW_REPORTED_HASHRATE,
        LOW_CURRENT_HASHRATE,
        OK;
    }

    @Override
    public String toString() {
        return "HealthResult{" +
            "lastSeen=" + lastSeen +
            ", health=" + health +
            ", message='" + message + '\'' +
            '}';
    }
}
