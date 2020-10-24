package br.com.rubim.runtime.filters;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.runtime.util.TagsUtil;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
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
            Metrics.dependencyRequestSeconds.labels(labels)
                .observe(Metrics.calcTimeElapsedInSeconds(init));
        }
    }
}
