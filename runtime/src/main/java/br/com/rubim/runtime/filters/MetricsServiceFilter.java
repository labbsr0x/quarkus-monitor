package br.com.rubim.runtime.filters;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.runtime.util.FilterUtils;
import br.com.rubim.runtime.util.TagsUtil;
import java.io.IOException;
import java.time.Instant;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class MetricsServiceFilter implements ContainerRequestFilter, ContainerResponseFilter {

  @Context
  UriInfo uriInfo;

  @Override
  public void filter(ContainerRequestContext request) throws IOException {
    if (FilterUtils.validPath(uriInfo)) {
      request.setProperty(FilterUtils.TIMER_INIT_TIME_MILLISECONDS, Instant.now());
    }
  }

  @Override
  public void filter(
      ContainerRequestContext containerRequestContext,
      ContainerResponseContext containerResponseContext)
      throws IOException {
    if (FilterUtils.validPath(uriInfo)) {
      var labels = TagsUtil.extractLabelValues(containerRequestContext, containerResponseContext);

      // Foi a forma que achei para passar o status code no aroundWriteTo
      containerRequestContext
          .setProperty(FilterUtils.STATUS_CODE, containerResponseContext.getStatus());

      if (containerRequestContext.getProperty(FilterUtils.TIMER_INIT_TIME_MILLISECONDS) != null) {
        Instant init = (Instant) containerRequestContext
            .getProperty(FilterUtils.TIMER_INIT_TIME_MILLISECONDS);

        Metrics.requestSeconds.labels(labels).observe(FilterUtils.calcTimeElapsedInSeconds(init));
      }
    }
  }
}
