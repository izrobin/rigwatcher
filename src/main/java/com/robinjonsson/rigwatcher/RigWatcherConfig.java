package com.robinjonsson.rigwatcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.validator.constraints.NotEmpty;

public class RigWatcherConfig extends Configuration {

    @NotEmpty
    @JsonProperty
    private String smsAccountId;
    @NotEmpty
    @JsonProperty
    private String smsAccessKey;

    @NotEmpty
    @JsonProperty
    private final List<String> alertNbrs = new ArrayList<>();
    @NotEmpty
    @JsonProperty
    private String minerId;

    public String getSmsAccountId() {
        return smsAccountId;
    }

    public String getSmsAccessKey() {
        return smsAccessKey;
    }

    public List<String> getAlertNbrs() {
        return alertNbrs;
    }

    public String getMinerId() {
        return minerId;
    }
}
