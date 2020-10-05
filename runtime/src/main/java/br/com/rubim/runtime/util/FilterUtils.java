package br.com.rubim.runtime.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
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
  private static final BigDecimal MULTIPLIER_NANO_TO_SECONDS = new BigDecimal(1.0E9D);

  private FilterUtils() {
  }

  public static boolean validPath(UriInfo uriInfo) {
    return exclusions.stream().noneMatch(path -> TagsUtil.excludePath(uriInfo, path));
  }

  public static Integer extractStatusCodeFromContext(WriterInterceptorContext context) {
    return Integer.valueOf(context.getProperty(STATUS_CODE).toString());
  }

  public static double calcTimeElapsedInSeconds(Instant init) {
    var finish = Instant.now();
    BigDecimal diff = new BigDecimal(Duration.between(init, finish).toNanos());
    return diff.divide(MULTIPLIER_NANO_TO_SECONDS, 9, RoundingMode.HALF_UP).doubleValue();
  }
}
