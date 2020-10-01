package br.com.rubim.runtime.filters;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.runtime.util.TagsUtil;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;

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
        var labels = TagsUtil.extractLabelValues(clientRequestContext, clientResponseContext);
        Method method = (Method) clientRequestContext.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");

        if (clientResponseContext.getStatus() >= 200 && clientResponseContext.getStatus() < 500) {
            Metrics.dependencyUp.labels(method.getDeclaringClass().getCanonicalName()).set(1);
        } else if (clientResponseContext.getStatus() >= 500) {
            Metrics.dependencyUp.labels(method.getDeclaringClass().getCanonicalName()).set(0);
        }
        if (clientRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS) != null) {
            Instant init = (Instant) clientRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS);
            var duration = Duration.between(init, Instant.now()).toSeconds();
            Metrics.dependencyRequestSeconds.labels(labels).observe(duration);
        }
    }
}
