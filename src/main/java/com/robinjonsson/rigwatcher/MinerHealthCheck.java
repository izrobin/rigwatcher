package com.robinjonsson.rigwatcher;

import static com.robinjonsson.rigwatcher.HealthResult.Health.ALMOST_NO_HASHRATE;
import static com.robinjonsson.rigwatcher.HealthResult.Health.API_ERROR;
import static com.robinjonsson.rigwatcher.HealthResult.Health.HIGH_STALES;
import static com.robinjonsson.rigwatcher.HealthResult.Health.LOW_CURRENT_HASHRATE;
import static com.robinjonsson.rigwatcher.HealthResult.Health.LOW_REPORTED_HASHRATE;
import static com.robinjonsson.rigwatcher.HealthResult.Health.MISSING_IN_ACTION;
import static com.robinjonsson.rigwatcher.HealthResult.Health.OK;

import com.robinjonsson.rigwatcher.dto.EtherpoolMinerStatsWrapper;
import com.robinjonsson.rigwatcher.dto.EtherpoolMinerStatsWrapper.Statistics;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinerHealthCheck implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MinerHealthCheck.class);

    private final WebTarget etherpoolTarget;
    private final List<String> recipients = new ArrayList<>();

    private Instant lastHealthy;
    private Instant lastPoolResponse;
    private boolean reminderAlertSent;
    private HealthResult lastResult;

    public MinerHealthCheck(
        final Environment environment,
        final RigWatcherConfig config
    ) {
        this.lastHealthy = Instant.now();
        this.lastPoolResponse = Instant.now();
        this.recipients.addAll(config.getAlertNbrs());
        this.etherpoolTarget = new JerseyClientBuilder(environment)
            .build("EthermineClient")
            .target("https://api.ethermine.org")
            .path("miner")
            .path(config.getMinerId())
            .path("currentStats");
    }

    @Override
    public void run() {
        LOG.info("Running healthcheck...");

        final HealthResult health = getHealth();

        LOG.info("Current health: {}", health);

        if (lastResult == null) {
            lastResult = health;
        }

        //Change in health status! Send alerts
        if (health.getHealth() != lastResult.getHealth()) {
            sendAlerts(health.getMessage());
        }

        //We're feeling OK. Let's update our self
        if (HealthResult.Health.OK == health.getHealth()) {
            lastHealthy = Instant.now();
            reminderAlertSent = false;
        }

        if (timeHasPassedSince(lastHealthy, 180) && !reminderAlertSent) {
            sendAlerts("URGENT! Unhealthy rig status for more than 3 hours! Please take a look");
            reminderAlertSent = true;
        }

        lastResult = health;
    }

    private void sendAlerts(final String message) {
        for (final String recipient : recipients) {
            final Message alert = Message.creator(
                new PhoneNumber(recipient),
                new PhoneNumber("RigWatcher"),
                message
            ).create();

            LOG.info("Sent alert to={} message={}", alert.getTo(), alert.getBody());
        }
    }

    private HealthResult getHealth() {
        try {
            final Statistics stats = getStats();
            this.lastPoolResponse = Instant.now();

            return examineStats(stats);
        } catch (final Exception e) {
            if (timeHasPassedSince(lastPoolResponse, 30)) {
                return new HealthResult(
                    null,
                    API_ERROR,
                    "URGENT! No statistics found for the last 30 minutes. Pool might be down. Message: "
                        + e.getMessage()
                );
            }
            return lastResult;
        }
    }

    private HealthResult examineStats(final Statistics stats) {
        if (stats.getActiveWorkers() == 0) {
            return new HealthResult(
                stats.getLastSeen(),
                MISSING_IN_ACTION,
                "URGENT! 0 active workers reported! We might be offline!"
            );
        }

        if (stats.stalesPercentage() > 0.10d) {
            return new HealthResult(
                stats.getLastSeen(),
                HIGH_STALES,
                "Warning! High % of stale shares. Stales percentage: " +
                    round(stats.stalesPercentage())
            );
        }

        if (stats.getReportedHashrate() < 125_000_000L) {
            return new HealthResult(
                stats.getLastSeen(),
                ALMOST_NO_HASHRATE,
                "URGENT! Reported hashrate below 50% of regular value! Reported hashrate (MH/s): " +
                    round(stats.getReportedHashrate() / 1_000_000L)
            );
        }

        if (stats.getReportedHashrate() < 230_000_000L) {
            return new HealthResult(
                stats.getLastSeen(),
                LOW_REPORTED_HASHRATE,
                "Warning! Reported hashrate is below 230MH/s. Reported hashrate (MH/s): " +
                    round(stats.getReportedHashrate() / 1_000_000)
            );
        }

        if (stats.getCurrentHashrate() < 190_000_000L) {
            return new HealthResult(
                stats.getLastSeen(),
                LOW_CURRENT_HASHRATE,
                "Warning! Average (Last 60min) hashrate is below 190MH/s. Current hashrate (MH/s): " +
                    round(stats.getCurrentHashrate() / 1_000_000)
            );
        }

        final Duration sinceLastHealthy = Duration.between(lastHealthy, Instant.now());
        DurationFormatUtils.formatDurationHMS(sinceLastHealthy.toMillis());

        return new HealthResult(
            stats.getLastSeen(),
            OK,
            "OK! Status is back to normal. Outage duration: "
                + DurationFormatUtils.formatDurationHMS(sinceLastHealthy.toMillis())
        );
    }

    private boolean timeHasPassedSince(final Instant since, final long minutes) {
        final Instant offset = Instant.now().minus(minutes, ChronoUnit.MINUTES);
        return since.isBefore(offset);
    }

    public Statistics getStats() {
        return etherpoolTarget
            .request()
            .get(EtherpoolMinerStatsWrapper.class)
            .getData();
    }

    private static double round(final double value) {
        return BigDecimal
            .valueOf(value)
            .setScale(3, BigDecimal.ROUND_UP)
            .doubleValue();
    }
}
