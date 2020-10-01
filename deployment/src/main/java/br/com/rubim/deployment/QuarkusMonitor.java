package br.com.rubim.deployment;

import br.com.rubim.runtime.config.DependencyHealth;
import br.com.rubim.runtime.config.MetricsB5Configuration;
import br.com.rubim.runtime.filters.MetricsClientFilter;
import br.com.rubim.runtime.filters.MetricsExporter;
import br.com.rubim.runtime.filters.MetricsServiceFilter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import org.jboss.jandex.DotName;

class QuarkusMonitor {
    private static final DotName DEPENDENCY_HEALTH = DotName.createSimple(DependencyHealth.class.getName());

    private static final String FEATURE = "monitor";

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .build();
    }

    @BuildStep
    void createRoute(
            BuildProducer<FilterBuildItem> filterProducer) {
        filterProducer.produce(new FilterBuildItem(new MetricsExporter(),400));
    }

    @BuildStep
    void addProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
            MetricsB5Configuration configuration) {
        if (configuration.enable) {
            providers.produce(new ResteasyJaxrsProviderBuildItem(MetricsServiceFilter.class.getName()));
            providers.produce(new ResteasyJaxrsProviderBuildItem(MetricsClientFilter.class.getName()));
        }

    }

}
