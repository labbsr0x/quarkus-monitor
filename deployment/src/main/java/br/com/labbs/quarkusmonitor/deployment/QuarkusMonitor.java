package br.com.labbs.quarkusmonitor.deployment;

import br.com.labbs.quarkusmonitor.runtime.config.MetricsB5Configuration;
import br.com.labbs.quarkusmonitor.runtime.core.StartMetrics;
import br.com.labbs.quarkusmonitor.runtime.filters.MetricsClientFilter;
import br.com.labbs.quarkusmonitor.runtime.filters.MetricsServiceFilter;
import br.com.labbs.quarkusmonitor.runtime.filters.MetricsServiceInterceptor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class QuarkusMonitor {

  private static final String FEATURE = "quarkus-monitor";

  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(FEATURE);
  }
  
  @BuildStep
  AdditionalBeanBuildItem registerAdditionalBeans() {
    return new AdditionalBeanBuildItem.Builder()
        .setUnremovable()
        .addBeanClass(StartMetrics.class)
        .build();
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
