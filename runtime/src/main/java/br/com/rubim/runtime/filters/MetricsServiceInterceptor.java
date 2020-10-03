package br.com.rubim.runtime.filters;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.runtime.util.FilterUtils;
import br.com.rubim.runtime.util.TagsUtil;
import java.io.IOException;
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

    if (FilterUtils.validPath(uriInfo)) {
      context.setOutputStream(outputStream);
      context.proceed();
      var labels = TagsUtil
          .extractLabelValues(uriInfo, request,
              Integer.valueOf(context.getProperty(FilterUtils.STATUS_CODE).toString())
              , context);
      Metrics.responseSizeBytes.labels(labels).inc(outputStream.getByteCount());
    } else {
      context.proceed();
    }
  }
}
