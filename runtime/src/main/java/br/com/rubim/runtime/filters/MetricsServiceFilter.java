package br.com.rubim.runtime.filters;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.runtime.util.FilterUtils;
import br.com.rubim.runtime.util.TagsUtil;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class MetricsServiceFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final String IS_VALID_PATH_FOR_METRICS = "isValidPathForMetrics";

  @Override
  public void filter(ContainerRequestContext request) throws IOException {
    var isValid = FilterUtils.validPath(request.getUriInfo());
    request.setProperty(IS_VALID_PATH_FOR_METRICS, isValid);
    if (isValid) {
      request.setProperty(FilterUtils.TIMER_INIT_TIME_MILLISECONDS, Instant.now());
    }
  }

  @Override
  public void filter(
      ContainerRequestContext containerRequestContext,
      ContainerResponseContext containerResponseContext)
      throws IOException {

    if (getValidPathFromRequest(containerRequestContext)) {
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

  private boolean getValidPathFromRequest(ContainerRequestContext request) {
    return Boolean.valueOf(
        Optional.ofNullable(request.getProperty(IS_VALID_PATH_FOR_METRICS)).orElse("").toString());
  }
}
