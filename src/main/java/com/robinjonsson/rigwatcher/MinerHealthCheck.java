package com.robinjonsson.rigwatcher;

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
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinerHealthCheck implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MinerHealthCheck.class);

    private final WebTarget etherpoolTarget;
    private final List<String> recipients = new ArrayList<>();
    private HealthResult lastResult;

    public MinerHealthCheck(
        final Environment environment,
        final RigWatcherConfig config
    ) {
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

        if (lastResult == null || health.getHealth() != lastResult.getHealth()) {
            for (final String recipient : recipients) {
                final Message alert = Message.creator(
                    new PhoneNumber(recipient),
                    new PhoneNumber("RigWatcher"),
                    health.getMessage()
                ).create();

                LOG.info("Sent alert to={} message={}", alert.getTo(), alert.getBody());
            }
        }

        lastResult = health;
    }

    public HealthResult getHealth() {
        try {
            final Statistics stats = getStats();
            return examineStats(stats);
        } catch (final Exception e) {
            return new HealthResult(
                null,
                API_ERROR,
                "URGENT! Failed to query for statistics. Message: " + e.getMessage()
            );
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
                "Warning! High % of stale shares. Stales percentage: " + stats.stalesPercentage()
            );
        }

        if (stats.getReportedHashrate() < 230_000_000L) {
            return new HealthResult(
                stats.getLastSeen(),
                LOW_REPORTED_HASHRATE,
                "Warning! Reported hashrate is below 230MH/s. Reported hashrate (MH/s): " +
                    (stats.getReportedHashrate() / 1_000_000)
            );
        }

        if (stats.getCurrentHashrate() < 190_000_000L) {
            return new HealthResult(
                stats.getLastSeen(),
                LOW_CURRENT_HASHRATE,
                "Warning! Average (Last 60min) hashrate is below 190MH/s. Current hashrate (MH/s): " +
                    (stats.getCurrentHashrate() / 1_000_000)
            );
        }

        return new HealthResult(
            stats.getLastSeen(),
            OK,
            "OK! Status is back to normal."
        );
    }

    public Statistics getStats() {
        return etherpoolTarget
            .request()
            .get(EtherpoolMinerStatsWrapper.class)
            .getData();
    }
}
