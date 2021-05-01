package br.com.labbs.quarkusmonitor.runtime.filters;

import br.com.labbs.quarkusmonitor.runtime.MonitorMetrics;
import br.com.labbs.quarkusmonitor.runtime.core.Metrics;
import br.com.labbs.quarkusmonitor.runtime.util.TagsUtil;
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
    public void filter(ClientRequestContext clientRequestContext) {
        clientRequestContext.setProperty(TIMER_INIT_TIME_MILLISECONDS, Instant.now());
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) {
        var labels = TagsUtil.extractLabelValues(clientRequestContext, clientResponseContext);
        Method method = (Method) clientRequestContext.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");

        if (clientResponseContext.getStatus() >= 200 && clientResponseContext.getStatus() < 500) {
            Metrics.dependencyUp(method.getDeclaringClass().getCanonicalName());
        } else if (clientResponseContext.getStatus() >= 500) {
            Metrics.dependencyDown(method.getDeclaringClass().getCanonicalName());
        }

        if (clientRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS) != null) {
            Instant init = (Instant) clientRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS);
            Metrics.dependencyRequestSeconds(labels,
                MonitorMetrics.calcTimeElapsedInSeconds(init));
        }
    }
}
