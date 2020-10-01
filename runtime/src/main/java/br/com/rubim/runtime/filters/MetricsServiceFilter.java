package br.com.rubim.runtime.filters;

import br.com.rubim.runtime.config.MetricsEnum;
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

@Provider
public class MetricsServiceFilter implements ContainerRequestFilter, ContainerResponseFilter {
    static final Counter applicationInfo = Counter.build()
            .name(MetricsEnum.APPLICATION_INFO.getName().toLowerCase())
            .help(MetricsEnum.APPLICATION_INFO.getDescription())
            .labelNames("version")
            .register();
    private static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS";
    static Counter requestSecondsCount = Counter.build()
            .name(MetricsEnum.REQUEST_SECONDS_COUNT.getName().toLowerCase())
            .help(MetricsEnum.REQUEST_SECONDS_COUNT.getDescription())
            .labelNames("type", "method", "addr", "status", "isError")
            .register();

    static Counter requestSecondsSum = Counter.build()
            .name(MetricsEnum.REQUEST_SECONDS_SUM.getName().toLowerCase())
            .help(MetricsEnum.REQUEST_SECONDS_SUM.getDescription())
            .labelNames("type", "method", "addr", "status", "isError")
            .register();

    static Counter responseSizeBytes = Counter.build()
            .name(MetricsEnum.RESPONSE_SIZE_BYTES.getName().toLowerCase())
            .help(MetricsEnum.RESPONSE_SIZE_BYTES.getDescription())
            .labelNames("type", "method", "addr", "status", "isError")
            .register();
    private static double[] DEFAULT_BUCKETS = { 0.1D, 0.3D, 1.5D, 10.5D };
    static Histogram requestSecondsBucket = Histogram.build().name(MetricsEnum.REQUEST_SECONDS_BUCKET.getName())
            .help(MetricsEnum.REQUEST_SECONDS_BUCKET.getName())
            .labelNames("type", "status", "method", "addr", "isError")
            .buckets(DEFAULT_BUCKETS)
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
        requestSecondsCount.labels(labels).inc();
        if (containerResponseContext.getLength() != -1) {
            responseSizeBytes.labels(labels).inc(containerResponseContext.getLength());
        }
        if (containerRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS) != null) {
            Instant init = (Instant) containerRequestContext.getProperty("TIMER_INIT_TIME_MILLISECONDS");
            var duration = Duration.between(init, Instant.now()).toSeconds();
            requestSecondsSum.labels(labels).inc(duration);
            requestSecondsBucket.labels(labels).observe(duration);
        }
    }

}
