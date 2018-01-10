package com.robinjonsson.rigwatcher.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public class EtherpoolMinerStatsWrapper {

    @JsonProperty
    private Statistics data;
    @JsonProperty
    private String status;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Statistics {
        @JsonProperty
        private long time;
        @JsonProperty
        private long lastSeen;
        @JsonProperty
        private double reportedHashrate;
        @JsonProperty
        private double currentHashrate;
        @JsonProperty
        private int validShares;
        @JsonProperty
        private int invalidShares;
        @JsonProperty
        private int staleShares;
        @JsonProperty
        private double averageHashrate;
        @JsonProperty
        private int activeWorkers;

        public Instant getTime() {
            return Instant.ofEpochSecond(time);
        }

        public Instant getLastSeen() {
            return Instant.ofEpochSecond(lastSeen);
        }

        public double getReportedHashrate() {
            return reportedHashrate;
        }

        public double getCurrentHashrate() {
            return currentHashrate;
        }

        public int getValidShares() {
            return validShares;
        }

        public int getInvalidShares() {
            return invalidShares;
        }

        public int getStaleShares() {
            return staleShares;
        }

        public double getAverageHashrate() {
            return averageHashrate;
        }

        public int getActiveWorkers() {
            return activeWorkers;
        }

        public int stalesPercentage() {
            final int totalShares = staleShares + validShares + invalidShares;
            if (totalShares > 0) {
                final BigDecimal percentage = BigDecimal.valueOf(staleShares).divide(
                    BigDecimal.valueOf(totalShares), 2, RoundingMode.UP
                );
                return percentage
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.UP)
                    .intValue();
            }
            return 0;
        }
    }

    public Statistics getData() {
        return data;
    }
}
