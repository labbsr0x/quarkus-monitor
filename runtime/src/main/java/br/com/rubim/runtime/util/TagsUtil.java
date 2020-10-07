package br.com.rubim.runtime.util;

import java.lang.reflect.Method;
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
      .getValue("quarkus.b5.monitor.error-message", String.class);

  private TagsUtil() {
  }

  public static String[] extractLabelValues(ContainerRequestContext request,
      ContainerResponseContext response) {
    return new String[]{
        HTTP,
        Integer.toString(response.getStatus()),
        request.getMethod(),
        FilterUtils.replacePathParamValueForPathParamId(request.getUriInfo()),
        Boolean.toString(isError(response.getStatus())),
        extractMessageError(request, response)
    };
  }

  public static String[] extractLabelValues(ClientRequestContext request,
      ClientResponseContext response) {
    Method method = (Method) request
        .getProperty("org.eclipse.microprofile.rest.client.invokedMethod");

    return new String[]{
        method.getDeclaringClass().getCanonicalName(),
        HTTP,
        Integer.toString(response.getStatus()),
        request.getMethod(),
        request.getUri().getPath(),
        Boolean.toString(isError(response.getStatus())),
        extractMessageError(request, response)
    };
  }

  public static String[] extractLabelValues(UriInfo uriInfo, Request request,
      WriterInterceptorContext context) {
    int statusCode = FilterUtils.extractStatusCodeFromContext(context);
    return new String[]{
        HTTP,
        Integer.toString(statusCode),
        request.getMethod(),
        FilterUtils.replacePathParamValueForPathParamId(uriInfo),
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
