package br.com.rubim.deployment;

import br.com.rubim.runtime.*;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;

class QuarkusMonitor {

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
        System.out.println(configuration.enable);
        if(configuration.enable) {
            producer.produce(
                    metric(MetricsEnum.INFO, MetricType.COUNTER,
                            new Tag("version", ConfigProvider.getConfig()
                                    .getOptionalValue("quarkus.application.version", String.class).orElse("not-set"))));
        }


    }

    private MetricBuildItem metric(MetricsEnum metric, MetricType type, Tag... tags) {
        return new MetricBuildItem(MetadataBuilder.Build(metric,type,tags),null, true, "", MetricRegistry.Type.APPLICATION,tags);
    }

}
