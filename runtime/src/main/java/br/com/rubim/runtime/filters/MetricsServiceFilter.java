package br.com.rubim.runtime.filters;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.runtime.util.TagsUtil;

import javax.inject.Inject;
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
    private static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS";

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        request.setProperty(TIMER_INIT_TIME_MILLISECONDS, Instant.now());
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext)
            throws IOException {
        var labels = TagsUtil.extractLabelValues(containerRequestContext, containerResponseContext);
        if (containerResponseContext.getLength() != -1) {
            Metrics.responseSizeBytes.labels(labels).inc(containerResponseContext.getLength());
        }
        if (containerRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS) != null) {
            Instant init = (Instant) containerRequestContext.getProperty("TIMER_INIT_TIME_MILLISECONDS");
            var duration = Duration.between(init, Instant.now()).toSeconds();
            Metrics.requestSeconds.labels(labels).observe(duration);
        }
    }

}
