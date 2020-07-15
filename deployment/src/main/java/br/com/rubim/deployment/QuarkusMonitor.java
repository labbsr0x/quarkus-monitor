package br.com.rubim.deployment;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

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
    }

    @BuildStep
    void registerMetrics(BuildProducer<MetricBuildItem> producer) {
        producer.produce(
                metric("application_info", MetricType.COUNTER, "static info of the application",
                        new Tag("version", ConfigProvider.getConfig()
                                .getOptionalValue("quarkus.application.version", String.class).orElse("not-set"))));

    }

    private MetricBuildItem metric(String name, MetricType type, String description, Tag... tags) {
        Metadata metadata = Metadata.builder()
                .withName(name)
                .withDisplayName(name)
                .withType(type)
                .withUnit("none")
                .withDescription(description)
                .reusable()
                .build();
        return new MetricBuildItem(metadata, true, "", tags);
    }

}
