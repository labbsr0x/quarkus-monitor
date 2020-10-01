package br.com.rubim.runtime.filters;

import br.com.rubim.runtime.util.TagsUtil;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Provider
public class MetricsServiceFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static String DEFAULT_BUCKETS_CONFIG = ConfigProvider.getConfig().getValue("quarkus.b5.monitor.buckets", String.class);
    private static double[] DEFAULT_BUCKETS = Arrays.stream(DEFAULT_BUCKETS_CONFIG.split(",")).mapToDouble(Double::parseDouble).toArray();

    static final Counter applicationInfo = Counter.build()
            .name("application_info")
            .help("holds static info of an application, such as it's semantic version number")
            .labelNames("version")
            .register();
    private static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS";

    static Histogram requestSeconds = Histogram.build().name("request_seconds")
            .help("records in a histogram the number of http requests and their duration in seconds")
            .labelNames("type", "status", "method", "addr", "isError")
            .buckets(DEFAULT_BUCKETS)
            .register();

    static Counter responseSizeBytes = Counter.build()
            .name("response_size_bytes")
            .help("is a counter that computes how much data is being sent back to the user for a given request type. It captures the response size from the content-length response header. If there is no such header, the value exposed as metric will be zero")
            .labelNames("type", "method", "addr", "status", "isError")
            .register();


    static {
        applicationInfo.labels(ConfigProvider.getConfig()
                .getOptionalValue("quarkus.application.version", String.class).orElse("not-set")).inc();
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        request.setProperty(TIMER_INIT_TIME_MILLISECONDS, Instant.now());
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext)
            throws IOException {
        var labels = TagsUtil.extractLabelValues(containerRequestContext, containerResponseContext);
        if (containerResponseContext.getLength() != -1) {
            responseSizeBytes.labels(labels).inc(containerResponseContext.getLength());
        }
        if (containerRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS) != null) {
            Instant init = (Instant) containerRequestContext.getProperty("TIMER_INIT_TIME_MILLISECONDS");
            var duration = Duration.between(init, Instant.now()).toSeconds();
            requestSeconds.labels(labels).observe(duration);
        }
    }

}
