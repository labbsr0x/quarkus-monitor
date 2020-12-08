package br.com.labbs.quarkusmonitor.runtime.core;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.inject.spi.CDI;
import org.eclipse.microprofile.config.ConfigProvider;

public class Metrics {

  private static final String TYPE = "type";
  private static final String STATUS = "status";
  private static final String METHOD = "method";
  private static final String ADDR = "addr";
  private static final String IS_ERROR = "isError";
  private static final String ERROR_MESSAGE = "errorMessage";
  private static final String NAME = "name";
  private static final String VERSION = "version";
  private static final List<String> tagsKeysRequest = Arrays
      .asList(TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE);
  private static final List<String> tagsKeysDependency = Arrays
      .asList(NAME, TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE);

  private static MeterRegistry registry = CDI.current().select(MeterRegistry.class).get();
  private static ConcurrentMap<String, AtomicInteger> gaugeMap = new ConcurrentHashMap<>();

  private static double[] bucketsValues = Arrays.stream(
      ConfigProvider.getConfig().getOptionalValue("quarkus.b5.monitor.buckets", String.class)
          .orElse("0.1, 0.3, 1.5, 10.5").split(","))
      .map(String::trim).mapToDouble(Double::parseDouble).toArray();

  private Metrics() {
  }

  /**
   * Create a dependency request metric in seconds with name dependency_request_seconds and tag
   * values for the following tag keys NAME, TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE
   *
   * @param tagsValues values in order for tag keys NAME, TYPE, STATUS, METHOD, ADDR, IS_ERROR,
   * ERROR_MESSAGE
   * @param seconds seconds elapsed to execute request.
   */
  public static void dependencyRequestSeconds(String[] tagsValues, double seconds) {
    createDistributionSummary("dependency_request_seconds",
        "records in a histogram the number of requests of a dependency and their duration in seconds",
        tagWithValue(tagsKeysDependency, tagsValues),
        seconds);
  }

  /**
   * Create a request metric in seconds with name request_seconds and tag values for the following
   * tag keys TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE
   *
   * @param tagsValues values in order for tag keys TYPE, STATUS, METHOD, ADDR, IS_ERROR,
   * ERROR_MESSAGE
   * @param seconds seconds elapsed to execute request.
   */
  public static void requestSeconds(String[] tagsValues, double seconds) {
    createDistributionSummary("request_seconds",
        "records in a histogram the number of http requests and their duration in seconds",
        tagWithValue(tagsKeysRequest, tagsValues),
        seconds);
  }

  private static void createDistributionSummary(String name, String description, Iterable<Tag> tags,
      double value) {
    Optional.ofNullable(registry.find(name).tags(tags).summary()).ifPresentOrElse(
        metric -> metric.record(value),
        () -> DistributionSummary.builder(name)
            .description(description)
            .tags(tags)
            .serviceLevelObjectives(bucketsValues)
            .register(registry).record(value)
    );
  }

  /**
   * Create a appliction info metric to show the version of application in the tag value
   *
   * @param version version of application
   */
  public static void applicationInfo(String version) {
    createCounter("application_info",
        "holds static info of an application, such as it's semantic version number",
        Collections.singletonList(Tag.of(VERSION, version)), 1.0d);
  }

  /**
   * Create a response size metric in bytes with name response_size_bytes and tag values for the
   * following tag keys TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE
   *
   * @param tagsValues values in order for tag keys TYPE, STATUS, METHOD, ADDR, IS_ERROR,
   * ERROR_MESSAGE
   * @param size size of response in bytes.
   */
  public static void responseSizeBytes(String[] tagsValues, double size) {
    createCounter("response_size_bytes",
        "is a counter that computes how much data is being sent back to the user for a given request type. It captures the response size from the content-length response header. If there is no such header, the value exposed as metric will be zero",
        tagWithValue(tagsKeysRequest, tagsValues), size);
  }

  private static void createCounter(String name, String description, Iterable<Tag> tags,
      double value) {
    Optional.ofNullable(registry.find(name).tags(tags).summary()).ifPresentOrElse(
        metric -> metric.record(value),
        () -> Counter.builder(name)
            .description(description)
            .tags(tags)
            .register(registry).increment(value)
    );
  }

  /**
   * Create a metric with name dependency_up to show if a dependency is up
   *
   * @param dependencyName name of dependency in tag value of metric
   */
  public static void dependencyUp(String dependencyName) {
    createGaugeDependency(Tag.of(NAME, dependencyName));
    gaugeMap.computeIfPresent(dependencyName, (key, value) -> {
      value.set(1);
      return value;
    });
    gaugeMap.computeIfAbsent(dependencyName, key -> new AtomicInteger(1));
  }

  /**
   * Create a metric with name dependency_up to show if a dependency is down
   *
   * @param dependencyName name of dependency in tag value of metric
   */
  public static void dependencyDown(String dependencyName) {
    createGaugeDependency(Tag.of(NAME, dependencyName));
    gaugeMap.computeIfPresent(dependencyName, (key, value) -> {
      value.set(0);
      return value;
    });
    gaugeMap.computeIfAbsent(dependencyName, key -> new AtomicInteger(0));
  }

  private static void createGaugeDependency(Tag tag) {
    if (registry.find("dependency_up").tags(Collections.singletonList(tag)).gauge() == null) {
      var value = new AtomicInteger(0);
      gaugeMap.put(tag.getValue(), value);
      Gauge.builder("dependency_up", value::get)
          .description(
              "is a metric to register weather a specific dependency is up (1) or down (0). The label name registers the dependency name")
          .tags(Collections.singletonList(tag)).register(registry);
    }
  }

  private static Iterable<Tag> tagWithValue(List<String> tagsKeys, String[] tagsValues) {
    var tagList = new ArrayList<Tag>();

    for (int i = 0; i < tagsKeys.size(); i++) {
      if (i < tagsValues.length) {
        tagList.add(Tag.of(tagsKeys.get(i), tagsValues[i]));
      } else {
        tagList.add(Tag.of(tagsKeys.get(i), ""));
      }
    }

    return tagList;
  }
}
