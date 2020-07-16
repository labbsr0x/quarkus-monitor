package br.com.rubim.runtime;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.metrics.*;

import io.smallrye.metrics.MetricRegistries;

@Provider
public class MetricsClientFilter implements ClientResponseFilter, ClientRequestFilter {
    private static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS";

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        clientRequestContext.setProperty(TIMER_INIT_TIME_MILLISECONDS, Instant.now());
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext)
            throws IOException {
        Method method = (Method) clientRequestContext.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");
        Tag[] tags = new Tag[] { new Tag("name", method.getDeclaringClass().getCanonicalName()) };
        MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(MetricsEnum.DEPENDENCY_REQUEST_SECONDS_COUNT.getName(), tags)
                .inc();
        Instant init = (Instant) clientRequestContext.getProperty("TIMER_INIT_TIME_MILLISECONDS");

        MetricRegistries.get(MetricRegistry.Type.VENDOR).timer(Metadata.builder()
                .withName(MetricsEnum.DEPENDENCY_REQUEST_SECONDS_SUM.getName())
                .withUnit(MetricUnits.SECONDS)
                .withType(MetricType.TIMER)
                .build(), tags).update(Duration.between(init, Instant.now()).toMillis(), TimeUnit.MILLISECONDS);
    }
}
