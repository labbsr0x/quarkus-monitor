package br.com.rubim.runtime.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.eclipse.microprofile.config.ConfigProvider;

public class FilterUtils {

  public static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS";
  public static final String STATUS_CODE = "STATUS_CODE";

  private static final Collection<String> exclusions = Arrays.stream(ConfigProvider.getConfig()
      .getValue("quarkus.b5.monitor.exclusions", String.class).split(","))
      .map(Object::toString).map(String::trim).collect(Collectors.toList());

  private FilterUtils() {
  }

  public static boolean validPath(UriInfo uriInfo) {
    return exclusions.stream().noneMatch(path -> TagsUtil.excludePath(uriInfo, path));
  }

  public static Integer extractStatusCodeFromContext(WriterInterceptorContext context) {
    return Integer.valueOf(context.getProperty(STATUS_CODE).toString());
  }
}
