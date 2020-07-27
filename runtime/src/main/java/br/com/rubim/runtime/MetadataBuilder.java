package br.com.rubim.runtime;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

public class MetadataBuilder {
    public static Metadata Build(MetricsEnum metric, MetricType type, Tag... tags) {
        return Metadata.builder()
                .withName(metric.getName())
                .withDisplayName(metric.getName())
                .withType(type)
                .withUnit("none")
                .withDescription(metric.getDescription())
                .reusable()
                .build();
    }
}
