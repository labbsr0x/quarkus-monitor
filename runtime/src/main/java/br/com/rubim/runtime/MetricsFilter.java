package br.com.rubim.runtime;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.metrics.*;

import io.smallrye.metrics.MetricRegistries;

@Provider
public class MetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS";

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        request.setProperty(TIMER_INIT_TIME_MILLISECONDS, Instant.now());
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext)
            throws IOException {
        Tag[] tags = extractTags(containerRequestContext, containerResponseContext);
        MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(MetricsEnum.REQUEST_SECONDS_COUNT.getName(), tags)
                .inc();
        Instant init = (Instant) containerRequestContext.getProperty("TIMER_INIT_TIME_MILLISECONDS");

        MetricRegistries.get(MetricRegistry.Type.VENDOR).timer(Metadata.builder()
                .withName(MetricsEnum.REQUEST_SECONDS_SUM.getName())
                .withUnit(MetricUnits.SECONDS)
                .withType(MetricType.TIMER)
                .build(), tags).update(Duration.between(init, Instant.now()).toMillis(), TimeUnit.MILLISECONDS);

    }

    private Tag[] extractTags(ContainerRequestContext containerRequestContext,
            ContainerResponseContext containerResponseContext) {
        return new Tag[] {
                new Tag("type", "http"),
                new Tag("method", containerRequestContext.getMethod()),
                new Tag("addr", containerRequestContext.getUriInfo().getPath()),
                new Tag("status", Integer.toString(containerResponseContext.getStatus())),
                new Tag("isError",
                        Boolean.toString(
                                containerResponseContext.getStatus() < 200 || containerResponseContext.getStatus() >= 400))
        };
    }
}
