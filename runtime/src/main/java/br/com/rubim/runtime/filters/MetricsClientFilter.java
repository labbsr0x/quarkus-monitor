package br.com.rubim.runtime.filters;

import br.com.rubim.runtime.util.TagsUtil;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Provider
public class MetricsClientFilter implements ClientResponseFilter, ClientRequestFilter {
    private static String DEFAULT_BUCKETS_CONFIG = ConfigProvider.getConfig().getValue("quarkus.b5.monitor.buckets", String.class);
    private static double[] DEFAULT_BUCKETS = Arrays.stream(DEFAULT_BUCKETS_CONFIG.split(",")).mapToDouble(Double::parseDouble).toArray();

    static final Gauge dependencyUp = Gauge.build()
            .name("dependency_up")
            .help("is a metric to register weather a specific dependency is up (1) or down (0). The label name registers the dependency name")
            .labelNames("name")
            .register();
    private static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS_CLIENT";
    static Histogram requestSeconds = Histogram.build().name("request_seconds")
            .help("records in a histogram the number of http requests and their duration in seconds")
            .labelNames("type", "status", "method", "addr", "isError")
            .buckets(DEFAULT_BUCKETS)
            .register();

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        clientRequestContext.setProperty(TIMER_INIT_TIME_MILLISECONDS, Instant.now());
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext)
            throws IOException {
        var labels = TagsUtil.extractLabelValues(clientRequestContext, clientResponseContext);
        if (clientResponseContext.getStatus() >= 200 && clientResponseContext.getStatus() < 500) {
            dependencyUp.set(1);
        } else if (clientResponseContext.getStatus() >= 500) {
            dependencyUp.set(0);
        }
        if (clientRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS) != null) {
            Instant init = (Instant) clientRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS);
            var duration = Duration.between(init, Instant.now()).toSeconds();
            requestSeconds.labels(labels).observe(duration);
        }
    }
}
