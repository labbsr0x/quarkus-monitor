package br.com.rubim.runtime.filters;

import br.com.rubim.runtime.config.MetricsEnum;
import br.com.rubim.runtime.util.TagsUtil;
import io.smallrye.metrics.MetricRegistries;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Provider
public class MetricsClientFilter implements ClientResponseFilter, ClientRequestFilter {
    private static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS_CLIENT";

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        clientRequestContext.setProperty(TIMER_INIT_TIME_MILLISECONDS, Instant.now());
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext)
            throws IOException {
        Tag[] tags = TagsUtil.extractTags(clientRequestContext, clientResponseContext);

        MetricRegistries.get(MetricRegistry.Type.VENDOR)
                .counter(MetricsEnum.DEPENDENCY_REQUEST_SECONDS_COUNT.getDefaultMetadata(), tags)
                .inc();
        var gaugeDepsUp = MetricRegistries.get(MetricRegistry.Type.VENDOR)
                .concurrentGauge(MetricsEnum.DEPENDENCY_UP.getDefaultMetadata(), tags);

        if (clientResponseContext.getStatus() >= 200 && clientResponseContext.getStatus() < 500
                && gaugeDepsUp.getCount() == 0) {
            gaugeDepsUp.inc();
        } else if (clientResponseContext.getStatus() >= 500 && gaugeDepsUp.getCount() == 1) {
            gaugeDepsUp.dec();
        }
        MetricRegistries.get(MetricRegistry.Type.VENDOR)
                .counter(MetricsEnum.DEPENDENCY_REQUEST_SECONDS_COUNT.getDefaultMetadata(),tags)
                .inc();
        if (clientRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS) != null) {
            Instant init = (Instant) clientRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS);
            var duration = Duration.between(init, Instant.now()).toSeconds();

            MetricRegistries.get(MetricRegistry.Type.VENDOR)
                    .counter(MetricsEnum.DEPENDENCY_REQUEST_SECONDS_SUM.getDefaultMetadata(),tags)
                    .inc(duration);
            MetricRegistries.get(MetricRegistry.Type.VENDOR)
                    .timer(MetricsEnum.DEPENDENCY_REQUEST_SECONDS_BUCKET.getDefaultMetadata(), tags)
                    .update(duration,TimeUnit.SECONDS);
        }
    }
}
