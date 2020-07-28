package br.com.rubim.deployment;

import br.com.rubim.runtime.config.DependencyHealth;
import br.com.rubim.runtime.config.MetricsB5Configuration;
import br.com.rubim.runtime.config.MetricsEnum;
import br.com.rubim.runtime.filters.MetricsClientFilter;
import br.com.rubim.runtime.filters.MetricsServiceFilter;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class QuarkusMonitor {
    private static final DotName DEPENDENCY_HEALTH = DotName.createSimple(DependencyHealth.class.getName());


    private static final String FEATURE = "monitor";

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .build();
    }

    @BuildStep
    void addProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
            MetricsB5Configuration configuration) {
        if(configuration.enable){
            providers.produce(new ResteasyJaxrsProviderBuildItem(MetricsServiceFilter.class.getName()));
            providers.produce(new ResteasyJaxrsProviderBuildItem(MetricsClientFilter.class.getName()));
        }

    }

    @BuildStep
    void registerMetrics(BuildProducer<MetricBuildItem> producer, MetricsB5Configuration configuration) {
        if(configuration.enable) {
            producer.produce(
                    metric(MetricsEnum.APPLICATION_INFO, MetricType.COUNTER,
                            new Tag("version", ConfigProvider.getConfig()
                                    .getOptionalValue("quarkus.application.version", String.class).orElse("not-set"))));
        }


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
