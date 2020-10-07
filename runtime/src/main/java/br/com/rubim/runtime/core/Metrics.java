package br.com.rubim.runtime.core;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import java.util.Arrays;
import org.eclipse.microprofile.config.ConfigProvider;

public class Metrics {

  public static final Histogram requestSeconds;
  public static final Histogram dependencyRequestSeconds;
  public static final Counter responseSizeBytes;
  public static final Gauge dependencyUp;
  public static final Counter applicationInfo;

  private static final String TYPE = "type";
  private static final String STATUS = "status";
  private static final String METHOD = "method";
  private static final String ADDR = "addr";
  private static final String IS_ERROR = "isError";
  private static final String ERROR_MESSAGE = "errorMessage";
  private static final String NAME = "name";

  static {
    double[] bucketsValues = Arrays.stream(
        ConfigProvider.getConfig().getOptionalValue("quarkus.b5.monitor.buckets", String.class)
            .orElse("0.1, 0.3, 1.5, 10.5").split(","))
        .map(String::trim).mapToDouble(Double::parseDouble).toArray();
    applicationInfo = Counter.build()
        .name("application_info")
        .help("holds static info of an application, such as it's semantic version number")
        .labelNames("version")
        .register();

    responseSizeBytes = Counter.build()
        .name("response_size_bytes")
        .help(
            "is a counter that computes how much data is being sent back to the user for a given request type. It captures the response size from the content-length response header. If there is no such header, the value exposed as metric will be zero")
        .labelNames(TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE)
        .register();

    requestSeconds = Histogram.build().name("request_seconds")
        .help("records in a histogram the number of http requests and their duration in seconds")
        .labelNames(TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE)
        .buckets(bucketsValues)
        .register();

    dependencyUp = Gauge.build()
        .name("dependency_up")
        .help(
            "is a metric to register weather a specific dependency is up (1) or down (0). The label name registers the dependency name")
        .labelNames(NAME)
        .register();

    dependencyRequestSeconds = Histogram.build().name("dependency_request_seconds")
        .help(
            "records in a histogram the number of requests of a dependency and their duration in seconds")
        .labelNames(NAME, TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE)
        .buckets(bucketsValues)
        .register();
  }

  private Metrics() {
  }

}
