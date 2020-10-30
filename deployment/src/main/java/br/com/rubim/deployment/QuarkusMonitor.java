package br.com.rubim.deployment;

import br.com.rubim.runtime.config.MetricsB5Configuration;
import br.com.rubim.runtime.core.StartMetrics;
import br.com.rubim.runtime.filters.MetricsClientFilter;
import br.com.rubim.runtime.filters.MetricsExporter;
import br.com.rubim.runtime.filters.MetricsServiceFilter;
import br.com.rubim.runtime.filters.MetricsServiceInterceptor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;

class QuarkusMonitor {

  @BuildStep
  AdditionalBeanBuildItem registerAdditionalBeans() {
    return new AdditionalBeanBuildItem.Builder()
        .setUnremovable()
        .addBeanClass(StartMetrics.class)
        .build();
  }

  @BuildStep
  void createRoute(
      BuildProducer<FilterBuildItem> filterProducer) {
    filterProducer.produce(new FilterBuildItem(new MetricsExporter(), Integer.MAX_VALUE));
  }

  @BuildStep
  void addProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
      MetricsB5Configuration configuration) {
    if (configuration.enable) {
      providers.produce(new ResteasyJaxrsProviderBuildItem(MetricsServiceFilter.class.getName()));
      providers.produce(new ResteasyJaxrsProviderBuildItem(MetricsClientFilter.class.getName()));

    }

    if (configuration.enable && configuration.enableHttpResponseSize) {
      providers
          .produce(new ResteasyJaxrsProviderBuildItem(MetricsServiceInterceptor.class.getName()));
    }
  }
}
