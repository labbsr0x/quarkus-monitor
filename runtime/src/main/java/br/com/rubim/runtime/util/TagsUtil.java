package br.com.rubim.runtime.util;

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
  private static final String errorMessageKey = ConfigProvider.getConfig()
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
    if (response.getHeaderString(errorMessageKey) != null) {
      return response.getHeaderString(errorMessageKey);
    }

    if (request.getProperty(errorMessageKey) != null) {
      return request.getProperty(errorMessageKey).toString();
    }
    return "";
  }

  private static String extractMessageError(WriterInterceptorContext context) {
    if (context.getHeaders().containsKey(errorMessageKey)) {
      return context.getHeaders().get(errorMessageKey).get(0).toString();
    }

    if (context.getProperty(errorMessageKey) != null) {
      return context.getProperty(errorMessageKey).toString();
    }
    return "";
  }

  private static String extractMessageError(ClientRequestContext request,
      ClientResponseContext response) {
    if (response.getHeaderString(errorMessageKey) != null) {
      return response.getHeaderString(errorMessageKey);
    }
    if (request.getProperty(errorMessageKey) != null) {
      return request.getProperty(errorMessageKey).toString();
    }
    return "";
  }

  private static boolean isError(int status) {
    return status < 200 || status >= 400;
  }
}
