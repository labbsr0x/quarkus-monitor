package br.com.labbs.quarkusmonitor.runtime.util;

import io.micrometer.core.instrument.config.NamingConvention;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Named;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.core.ResourceMethodInvoker;

public class FilterUtils {

  private static final Pattern tagKeyChars = Pattern.compile("[^a-zA-Z0-9_]");

  public static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS";
  public static final String STATUS_CODE = "STATUS_CODE";
  public static final String VALID_PATH_FOR_METRICS = "VALID_PATH_FOR_METRICS";
  public static final String PATH_WITH_PARAM_ID = "PATH_WITH_PARAM_ID";

  private static final Collection<String> exclusions =
      Arrays.stream(
          ConfigProvider.getConfig()
              .getOptionalValue("quarkus.b5.monitor.exclusions", String.class)
              .orElse("")
              .split(","))
          .map(Object::toString)
          .map(String::trim)
          .collect(Collectors.toList());
  private static final String REST_CLIENT_METHOD = "org.eclipse.microprofile.rest.client.invokedMethod";
  private static final String RESOURCE_METHOD_INVOKER = "org.jboss.resteasy.core.ResourceMethodInvoker";

  private FilterUtils() {
  }

  public static boolean validPath(String pathWithParamId) {
    return exclusions.stream().noneMatch(path -> path.equalsIgnoreCase(pathWithParamId));
  }

  public static Integer extractStatusCodeFromContext(WriterInterceptorContext context) {
    return Integer.valueOf(context.getProperty(STATUS_CODE).toString());
  }

  public static String extractClassNameFromMethod(ClientRequestContext request) {
    if (request.getProperty(REST_CLIENT_METHOD) instanceof Method) {
      var method = (Method) request.getProperty(REST_CLIENT_METHOD);
      var annotation = method.getDeclaringClass().getAnnotation(Named.class);

      if (annotation != null && !annotation.value().isBlank()) {
        return tagConvert(annotation.value());
      }

      return method.getDeclaringClass().getCanonicalName();
    }

    return "";
  }

  public static String toPathWithParamId(ClientRequestContext request) {
    Method method = null;

    if (request.getProperty(REST_CLIENT_METHOD) instanceof Method) {
      method = (Method) request.getProperty(REST_CLIENT_METHOD);
    }

    return extractPathWithParamFromMethod(method, request.getUri().getPath());
  }

  public static String toPathWithParamId(ContainerRequestContext request) {
    ResourceMethodInvoker resourceMethodInvoker;
    Method method = null;

    if (request.getProperty(RESOURCE_METHOD_INVOKER) instanceof ResourceMethodInvoker) {
      resourceMethodInvoker = (ResourceMethodInvoker) request.getProperty(RESOURCE_METHOD_INVOKER);
      method = resourceMethodInvoker.getMethod();
    }

    return extractPathWithParamFromMethod(method, request.getUriInfo().getPath());
  }

  private static String extractPathWithParamFromMethod(Method method, String defaultPath) {
    String pathWithParam = "";

    if (method == null) {
      return defaultPath;
    }

    if (method.getDeclaringClass().getAnnotation(Path.class) != null) {
      pathWithParam = method.getDeclaringClass().getAnnotation(Path.class).value();
    }

    if (method.getAnnotation(Path.class) != null) {
      pathWithParam = pathWithParam + method.getAnnotation(Path.class).value();
    }

    return pathWithParam.isEmpty() ? defaultPath : pathWithParam;
  }

  public static String tagConvert(String key) {
    String conventionKey = NamingConvention.snakeCase.tagKey(key);

    String sanitized = tagKeyChars.matcher(conventionKey).replaceAll("_");
    if (!Character.isLetter(sanitized.charAt(0))) {
      sanitized = "m_" + sanitized;
    }
    return sanitized;
  }
}
