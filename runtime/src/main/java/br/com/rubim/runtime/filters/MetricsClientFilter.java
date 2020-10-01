package br.com.rubim.runtime.filters;

import br.com.rubim.runtime.config.MetricsEnum;
import br.com.rubim.runtime.util.TagsUtil;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Provider
public class MetricsClientFilter implements ClientResponseFilter, ClientRequestFilter {
    static final Gauge dependencyUp = Gauge.build()
            .name(MetricsEnum.DEPENDENCY_UP.getName().toLowerCase())
            .help(MetricsEnum.DEPENDENCY_UP.getDescription())
            .labelNames("name")
            .register();
    static final Gauge dependencyRequestSecondsCount = Gauge.build()
            .name(MetricsEnum.DEPENDENCY_REQUEST_SECONDS_COUNT.getName().toLowerCase())
            .help(MetricsEnum.DEPENDENCY_REQUEST_SECONDS_COUNT.getDescription())
            .labelNames("name")
            .register();
    private static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS_CLIENT";
    static Counter dependencyRequestSecondsSum = Counter.build()
            .name(MetricsEnum.REQUEST_SECONDS_SUM.getName().toLowerCase())
            .help(MetricsEnum.REQUEST_SECONDS_SUM.getDescription())
            .labelNames("type", "method", "addr", "status", "isError")
            .register();
    private static double[] DEFAULT_BUCKETS = { 0.1D, 0.3D, 1.5D, 10.5D };
    static Histogram dependencyRequestSecondsBucket = Histogram.build().name(MetricsEnum.DEPENDENCY_REQUEST_SECONDS_BUCKET
            .getName())
            .help(MetricsEnum.DEPENDENCY_REQUEST_SECONDS_BUCKET.getName())
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
        dependencyRequestSecondsCount.labels(labels).inc();
        if (clientResponseContext.getStatus() >= 200 && clientResponseContext.getStatus() < 500) {
            dependencyUp.set(1);
        } else if (clientResponseContext.getStatus() >= 500) {
            dependencyUp.set(0);
        }
        if (clientRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS) != null) {
            Instant init = (Instant) clientRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS);
            var duration = Duration.between(init, Instant.now()).toSeconds();
            dependencyRequestSecondsSum.labels(labels).inc(duration);
            dependencyRequestSecondsBucket.labels(labels).observe(duration);
        }
    }
}
