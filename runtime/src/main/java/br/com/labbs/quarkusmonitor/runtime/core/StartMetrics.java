package br.com.labbs.quarkusmonitor.runtime.core;

import br.com.labbs.quarkusmonitor.runtime.config.B5NamingConvention;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.quarkus.runtime.StartupEvent;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class StartMetrics {

  @Inject
  PrometheusMeterRegistry prometheusMeterRegistry;

  void onStart(@Observes StartupEvent ev) {
    prometheusMeterRegistry.config().namingConvention(new B5NamingConvention());
    Metrics.applicationInfo(ConfigProvider.getConfig()
        .getOptionalValue("quarkus.application.version", String.class).orElse("not-set"));
  }
}
