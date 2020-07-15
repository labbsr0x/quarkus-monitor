package br.com.rubim.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class MetricsB5Configuration {
    /**
     * Enable the extension.
     */
    @ConfigItem(defaultValue = "true")
    boolean enable;
}
