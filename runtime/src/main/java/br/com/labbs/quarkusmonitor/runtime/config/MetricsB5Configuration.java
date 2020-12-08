package br.com.labbs.quarkusmonitor.runtime.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED, name = "b5.monitor")
public class MetricsB5Configuration {
    /**
     * Enable the extension.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enable;

    /**
     * Define the path where the metrics are exposed.
     */
    @ConfigItem(defaultValue = "/metrics")
    public String path;

    /**
     * Define the path where the metrics are exposed.
     */
    @ConfigItem(defaultValue = "0.1, 0.3, 1.5, 10.5")
    public String buckets;

    /**
     * Define the paths where the b5 metrics are not apply.
     */
    @ConfigItem(defaultValue = "/metrics")
    public String exclusions;

    /**
     * Define to turn on or off the http response size, default false
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableHttpResponseSize;

    /**
     * Define the key for error messages put in the request attribute
     */
    @ConfigItem(defaultValue = "error-info")
    public String errorMessage;
}
