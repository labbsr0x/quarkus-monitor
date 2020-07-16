package br.com.rubim.deployment;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import br.com.rubim.runtime.MetricsClientFilter;
import br.com.rubim.runtime.MetricsEnum;
import br.com.rubim.runtime.MetricsServiceFilter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;

class QuarkusMonitor {

    private static final String FEATURE = "dx-quarkus-ext";

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .build();
    }

    @BuildStep
    void addProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItem, Capabilities capabilities) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(MetricsServiceFilter.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(MetricsClientFilter.class.getName()));

    }

    @BuildStep
    void registerMetrics(BuildProducer<MetricBuildItem> producer) {
        producer.produce(
                metric(MetricsEnum.APPLICATION_INFO, MetricType.COUNTER,
                        new Tag("version", ConfigProvider.getConfig()
                                .getOptionalValue("quarkus.application.version", String.class).orElse("not-set"))));

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
