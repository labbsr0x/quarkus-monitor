package br.com.labbs.quarkusmonitor.runtime.filters;

import br.com.labbs.quarkusmonitor.runtime.core.Metrics;
import br.com.labbs.quarkusmonitor.runtime.util.FilterUtils;
import br.com.labbs.quarkusmonitor.runtime.util.TagsUtil;
import java.io.IOException;
import java.util.Optional;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.apache.commons.io.output.CountingOutputStream;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class MetricsServiceInterceptor implements WriterInterceptor {

  @Context
  UriInfo uriInfo;

  @Context
  Request request;

  @Override
  public void aroundWriteTo(WriterInterceptorContext context)
      throws IOException, WebApplicationException {
    CountingOutputStream outputStream
        = new CountingOutputStream(context.getOutputStream());

    if (getValidPathFromRequest(context)) {
      context.setOutputStream(outputStream);
      context.proceed();
      var labels = TagsUtil
          .extractLabelValues(uriInfo, request, context);
      Metrics.responseSizeBytes(labels, outputStream.getByteCount());
    } else {
      context.proceed();
    }
  }

  private boolean getValidPathFromRequest(WriterInterceptorContext context) {
    return Boolean.valueOf(
        Optional.ofNullable(context.getProperty(FilterUtils.VALID_PATH_FOR_METRICS))
            .orElse("").toString());
  }
}
