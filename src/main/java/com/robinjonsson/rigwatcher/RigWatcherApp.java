package com.robinjonsson.rigwatcher;

import com.twilio.Twilio;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RigWatcherApp extends Application<RigWatcherConfig> {

    public static void main(final String[] args) throws Exception {
        new RigWatcherApp().run(args);
    }

    @Override
    public void run(final RigWatcherConfig config, final Environment environment) throws Exception {
        Twilio.init(
            config.getSmsAccountId(),
            config.getSmsAccessKey()
        );

        final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
        executorService.scheduleAtFixedRate(
            new MinerHealthCheck(environment, config),
            0,
            2,
            TimeUnit.MINUTES
        );
    }
}
