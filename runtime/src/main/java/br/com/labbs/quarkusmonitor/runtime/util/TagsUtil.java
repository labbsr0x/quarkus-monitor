package br.com.labbs.quarkusmonitor.runtime.util;

import java.util.Optional;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.eclipse.microprofile.config.ConfigProvider;

public class TagsUtil {

  private static final String HTTP = "http";
  private static final String ERROR_MESSAGE_KEY = ConfigProvider.getConfig()
      .getOptionalValue("quarkus.b5.monitor.error-message", String.class).orElse("error-info");

  private TagsUtil() {
  }

  public static String[] extractLabelValues(ContainerRequestContext request,
      ContainerResponseContext response) {
    var pathWithParamId = (Optional.ofNullable(request.getProperty(FilterUtils.PATH_WITH_PARAM_ID))
        .orElse(request.getUriInfo().getPath())).toString();
    return new String[]{
        HTTP,
        Integer.toString(response.getStatus()),
        request.getMethod(),
        pathWithParamId,
        Boolean.toString(isError(response.getStatus())),
        extractMessageError(request, response)
    };
  }

  public static String[] extractLabelValues(ClientRequestContext request,
      ClientResponseContext response) {
    return new String[]{
        FilterUtils.extractClassNameFromMethod(request),
        HTTP,
        Integer.toString(response.getStatus()),
        request.getMethod(),
        FilterUtils.toPathWithParamId(request),
        Boolean.toString(isError(response.getStatus())),
        extractMessageError(request, response)
    };
  }

  public static String[] extractLabelValues(UriInfo uriInfo, Request request,
      WriterInterceptorContext context) {
    int statusCode = FilterUtils.extractStatusCodeFromContext(context);
    var pathWithParamId = (Optional.ofNullable(context.getProperty(FilterUtils.PATH_WITH_PARAM_ID))
        .orElse(uriInfo.getPath())).toString();
    return new String[]{
        HTTP,
        Integer.toString(statusCode),
        request.getMethod(),
        pathWithParamId,
        Boolean.toString(isError(statusCode)),
        extractMessageError(context)
    };
  }

  private static String extractMessageError(ContainerRequestContext request,
      ContainerResponseContext response) {
    if (response.getHeaderString(ERROR_MESSAGE_KEY) != null) {
      return response.getHeaderString(ERROR_MESSAGE_KEY);
    }

    if (request.getProperty(ERROR_MESSAGE_KEY) != null) {
      return request.getProperty(ERROR_MESSAGE_KEY).toString();
    }
    return "";
  }

  private static String extractMessageError(WriterInterceptorContext context) {
    if (context.getHeaders().containsKey(ERROR_MESSAGE_KEY)) {
      return context.getHeaders().get(ERROR_MESSAGE_KEY).get(0).toString();
    }

    if (context.getProperty(ERROR_MESSAGE_KEY) != null) {
      return context.getProperty(ERROR_MESSAGE_KEY).toString();
    }
    return "";
  }

  private static String extractMessageError(ClientRequestContext request,
      ClientResponseContext response) {
    if (response.getHeaderString(ERROR_MESSAGE_KEY) != null) {
      return response.getHeaderString(ERROR_MESSAGE_KEY);
    }
    if (request.getProperty(ERROR_MESSAGE_KEY) != null) {
      return request.getProperty(ERROR_MESSAGE_KEY).toString();
    }
    return "";
  }

  private static boolean isError(int status) {
    return status < 200 || status >= 400;
  }
}
