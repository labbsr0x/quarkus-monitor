package br.com.rubim.runtime.filters;

import br.com.rubim.runtime.config.MetricsEnum;
import br.com.rubim.runtime.util.TagsUtil;
import io.smallrye.metrics.MetricRegistries;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Provider
public class MetricsServiceFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS";

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        request.setProperty(TIMER_INIT_TIME_MILLISECONDS, Instant.now());
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext)
            throws IOException {
        Tag[] tags = TagsUtil.extractTags(containerRequestContext, containerResponseContext);
        MetricRegistries.get(MetricRegistry.Type.APPLICATION)
                .counter(MetricsEnum.REQUEST_SECONDS_COUNT.getDefaultMetadata(), tags)
                .inc();

        if (containerResponseContext.getLength() != -1) {
            MetricRegistries.get(MetricRegistry.Type.APPLICATION).counter(MetricsEnum.RESPONSE_SIZE_BYTES.getDefaultMetadata())
                    .inc(containerResponseContext.getLength());
        }
        MetricRegistries.get(MetricRegistry.Type.APPLICATION)
                .counter(MetricsEnum.REQUEST_SECONDS_COUNT.getDefaultMetadata(),tags)
                .inc();
        if (containerRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS) != null) {
            Instant init = (Instant) containerRequestContext.getProperty("TIMER_INIT_TIME_MILLISECONDS");
            var duration = Duration.between(init, Instant.now()).toMillis();
            MetricRegistries.get(MetricRegistry.Type.APPLICATION)
                    .counter(MetricsEnum.REQUEST_SECONDS_SUM.getDefaultMetadata(), tags)
                    .inc(duration);
            MetricRegistries.get(MetricRegistry.Type.APPLICATION)
                    .timer(MetricsEnum.REQUEST_SECONDS_BUCKET.getDefaultMetadata(), tags)
                    .update(Duration.between(init, Instant.now()).toMillis(), TimeUnit.MILLISECONDS);
        }
    }

}
