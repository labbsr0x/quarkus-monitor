package br.com.rubim.runtime.config;

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
}
