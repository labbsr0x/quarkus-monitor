package br.com.rubim.deployment;

import br.com.rubim.runtime.config.DependencyHealth;
import br.com.rubim.runtime.config.MetricsB5Configuration;
import br.com.rubim.runtime.config.MetricsEnum;
import br.com.rubim.runtime.filters.MetricsClientFilter;
import br.com.rubim.runtime.filters.MetricsServiceFilter;
import br.com.rubim.runtime.handlers.MetricsHandler;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
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
    void createRoute(BuildProducer<RouteBuildItem> routes,
            MetricsB5Configuration configuration) {
        routes.produce(new RouteBuildItem(configuration.path, new MetricsHandler(configuration.path), HandlerType.NORMAL));
    }

    @BuildStep
    void addProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
            MetricsB5Configuration configuration) {
        if (configuration.enable) {
            providers.produce(new ResteasyJaxrsProviderBuildItem(MetricsServiceFilter.class.getName()));
            providers.produce(new ResteasyJaxrsProviderBuildItem(MetricsClientFilter.class.getName()));
        }

    }

    @BuildStep
    void registerMetrics(BuildProducer<MetricBuildItem> producer, MetricsB5Configuration configuration) {
        //TODO metrics recorder
        //        if(configuration.enable) {
        //            producer.produce(
        //                    metric(MetricsEnum.APPLICATION_INFO, MetricType.COUNTER,
        //                            new Tag("kind","b5"),
        //                            new Tag("version", ConfigProvider.getConfig()
        //                                    .getOptionalValue("quarkus.application.version", String.class).orElse("not-set"))));
        //        }

    }

    private MetricBuildItem metric(MetricsEnum metric, MetricType type, Tag... tags) {
        Metadata metadata = Metadata.builder()
                .withName(metric.getName())
                .withDisplayName(metric.getName())
                .withType(type)
                .withUnit("none")
                .withDescription(metric.getDescription())
                .reusable()
                .build();
        return new MetricBuildItem(metadata, true, "", tags);
    }

}
