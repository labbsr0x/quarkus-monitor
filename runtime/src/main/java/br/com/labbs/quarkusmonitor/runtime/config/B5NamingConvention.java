package br.com.labbs.quarkusmonitor.runtime.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import java.util.regex.Pattern;

public class B5NamingConvention implements NamingConvention {

  private static final Pattern nameChars = Pattern.compile("[^a-zA-Z0-9_:]");
  private static final Pattern tagKeyChars = Pattern.compile("[^a-zA-Z0-9_]");
  public static final String SECONDS = "_seconds";
  private final String timerSuffix;

  public B5NamingConvention() {
    this("");
  }

  public B5NamingConvention(String timerSuffix) {
    this.timerSuffix = timerSuffix;
  }

  private boolean isNotB5MetricName(String name){
    return !name.startsWith("application_info") &&
        !name.startsWith("response_size_bytes") &&
        !name.startsWith("request_seconds") &&
        !name.startsWith("dependency_request_seconds");
  }

  /**
   * Names are snake-cased. They contain a base unit suffix when applicable.
   * <p>
   * Names may contain ASCII letters and digits, as well as underscores and colons. They must match the regex
   * [a-zA-Z_:][a-zA-Z0-9_:]*
   */
  @Override
  public String name(String name, Meter.Type type, String baseUnit) {
    String conventionName = NamingConvention.snakeCase.name(name, type, baseUnit);

    switch (type) {
      case COUNTER:
      case DISTRIBUTION_SUMMARY:
      case GAUGE:
        if (baseUnit != null && !conventionName.endsWith("_" + baseUnit) && isNotB5MetricName(
            conventionName))
          conventionName += "_" + baseUnit;
        break;
      default:
        break;
    }

    switch (type) {
      case COUNTER:
        if (!conventionName.endsWith("_total") && isNotB5MetricName(conventionName)) {
          conventionName += "_total";
        }
        break;
      case TIMER:
      case LONG_TASK_TIMER:
        if (conventionName.endsWith(timerSuffix) && isNotB5MetricName(conventionName)) {
          conventionName += SECONDS;
        } else if (!conventionName.endsWith(SECONDS) && isNotB5MetricName(conventionName))
          conventionName += timerSuffix + SECONDS;
        break;
      default:
        break;
    }

    String sanitized = nameChars.matcher(conventionName).replaceAll("_");
    if (!Character.isLetter(sanitized.charAt(0))) {
      sanitized = "m_" + sanitized;
    }
    return sanitized;
  }

  /**
   * Label names may contain ASCII letters, numbers, as well as underscores. They must match the regex
   * [a-zA-Z_][a-zA-Z0-9_]*. Label names beginning with __ are reserved for internal use.
   */
  @Override
  public String tagKey(String key) {
    return tagConvert(key);
  }

  public static String tagConvert(String key){
    String conventionKey = NamingConvention.snakeCase.tagKey(key);

    String sanitized = tagKeyChars.matcher(conventionKey).replaceAll("_");
    if (!Character.isLetter(sanitized.charAt(0))) {
      sanitized = "m_" + sanitized;
    }
    return sanitized;
  }
}
