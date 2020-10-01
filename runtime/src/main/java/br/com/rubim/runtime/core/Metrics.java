package br.com.rubim.runtime.core;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.util.Arrays;

public class Metrics {
    public static Histogram requestSeconds;
    public static Histogram dependencyRequestSeconds;
    public static Counter applicationInfo;
    public static Counter responseSizeBytes;
    public static Gauge dependencyUp;

    static {
        double[] bucketsValues = Arrays.stream(ConfigProvider.getConfig().getValue("quarkus.b5.monitor.buckets", String.class).split(",")).map(String::trim).mapToDouble(Double::parseDouble).toArray();
        applicationInfo = Counter.build()
                .name("application_info")
                .help("holds static info of an application, such as it's semantic version number")
                .labelNames("version")
                .register();
        responseSizeBytes = Counter.build()
                .name("response_size_bytes")
                .help("is a counter that computes how much data is being sent back to the user for a given request type. It captures the response size from the content-length response header. If there is no such header, the value exposed as metric will be zero")
                .labelNames("type", "method", "addr", "status", "isError")
                .register();

        requestSeconds = Histogram.build().name("request_seconds")
                .help("records in a histogram the number of http requests and their duration in seconds")
                .labelNames("type", "status", "method", "addr", "isError")
                .buckets(bucketsValues)
                .register();
        dependencyUp = Gauge.build()
                .name("dependency_up")
                .help("is a metric to register weather a specific dependency is up (1) or down (0). The label name registers the dependency name")
                .labelNames("name")
                .register();
        dependencyRequestSeconds = Histogram.build().name("dependency_request_seconds")
                .help("records in a histogram the number of requests of a dependency and their duration in seconds")
                .labelNames("type", "status", "method", "addr", "isError")
                .buckets(bucketsValues)
                .register();

        applicationInfo.labels(ConfigProvider.getConfig()
                .getOptionalValue("quarkus.application.version", String.class).orElse("not-set")).inc();
    }

}
