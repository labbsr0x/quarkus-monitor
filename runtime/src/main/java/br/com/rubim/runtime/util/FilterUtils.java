package br.com.rubim.runtime.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;

public class FilterUtils {

  public static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS";
  public static final String STATUS_CODE = "STATUS_CODE";
  public static final String VALID_PATH_FOR_METRICS = "VALID_PATH_FOR_METRICS";
  public static final String PATH_WITH_PARAM_ID = "PATH_WITH_PARAM_ID";


  private static final Collection<String> exclusions = Arrays.stream(ConfigProvider.getConfig()
      .getOptionalValue("quarkus.b5.monitor.exclusions", String.class).orElse("").split(","))
      .map(Object::toString).map(String::trim).collect(Collectors.toList());
  private static final BigDecimal MULTIPLIER_NANO_TO_SECONDS = new BigDecimal(1.0E9D);

  private FilterUtils() {
  }

//  private static boolean excludePath(UriInfo uriInfo, String pathToExclude) {
//    return replacePathParamValueForPathParamId(uriInfo).equalsIgnoreCase(pathToExclude);
//  }

  public static boolean validPath(String pathWithParamId) {
    return exclusions.stream().noneMatch(path -> path.equalsIgnoreCase(pathWithParamId));
  }

  public static Integer extractStatusCodeFromContext(WriterInterceptorContext context) {
    return Integer.valueOf(context.getProperty(STATUS_CODE).toString());
  }

  public static double calcTimeElapsedInSeconds(Instant init) {
    var finish = Instant.now();
    BigDecimal diff = new BigDecimal(Duration.between(init, finish).toNanos());
    return diff.divide(MULTIPLIER_NANO_TO_SECONDS, 9, RoundingMode.HALF_UP).doubleValue();
  }

  public static String replacePathParamValueForPathParamId(UriInfo uriInfo) {
    StringBuilder sb = new StringBuilder();

    Map<Integer, String> indices = new HashMap<>();
    var paramSegments = ((ResteasyUriInfo) uriInfo).getPathParameterPathSegments();

    for (Entry e : paramSegments.entrySet()) {
      PathSegment value = null;
      if (e.getValue() instanceof ArrayList) {
        value = ((PathSegment[]) ((ArrayList) e.getValue()).get(0))[0];
      }
      var idx = uriInfo.getPathSegments().indexOf(value);

      if (idx >= 0 && value != null) {
        indices.put(idx, "{" + e.getKey().toString() + "}");
      }
    }

    for (int idx = 0; idx < uriInfo.getPathSegments().size(); idx++) {
      sb.append("/");
      if (indices.containsKey(idx)) {
        sb.append(indices.get(idx));
      } else {
        sb.append(uriInfo.getPathSegments().get(idx).getPath());
      }
    }

    return sb.toString();
  }
}
