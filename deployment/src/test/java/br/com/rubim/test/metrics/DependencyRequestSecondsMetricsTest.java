package br.com.rubim.test.metrics;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.rubim.runtime.MonitorMetrics;
import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.runtime.dependency.DependencyEvent;
import br.com.rubim.test.fake.filters.MetricsFilterForError;
import br.com.rubim.test.fake.resources.DependencyResource;
import br.com.rubim.test.fake.resources.DependencyRestClient;
import io.quarkus.test.QuarkusUnitTest;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DependencyRequestSecondsMetricsTest {

  private static final String SIMPLE_PATH = "/dep/simple";

  @RegisterExtension
  static QuarkusUnitTest config = new QuarkusUnitTest()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
          .addClasses(DependencyResource.class, MetricsFilterForError.class,
              DependencyRestClient.class)
          .addAsResource(
              new StringAsset("quarkus.b5.monitor.enable-http-response-size=false\n" +
                  "br.com.rubim.test.fake.resources.DependencyRestClient/mp-rest/url=${test.url}"),
              "application.properties"));

  @RestClient
  DependencyRestClient restClient;

  @ConfigProperty(name = "quarkus.b5.monitor.error-message")
  String errorKey;

  @ConfigProperty(name = "quarkus.b5.monitor.buckets")
  List<String> buckets;


  @BeforeEach
  void cleanMetrics() {
    Metrics.dependencyRequestSeconds.clear();
  }

  @Test
  void testStructOfDependencyRequestMetric() {
    var tagValues = new String[]{DependencyRestClient.class.getName(),
        "http", "200", "GET", SIMPLE_PATH + "/{status}", "false", "", buckets.get(0)};

    var tagNames = new String[]{"name",
        "type", "status", "method", "addr", "isError", "errorMessage", "le"};

    restClient.simple(200);

    var samples = Metrics.dependencyRequestSeconds.collect().get(0).samples;

    assertEquals(samples.size(),
        buckets.size() + 3, "Metric dependency_request_seconds with wrong number of samples");

    var sampleBucket = samples.stream()
        .filter(sample -> "dependency_request_seconds_bucket".equals(sample.name)).findFirst();

    assertTrue(sampleBucket.isPresent(),
        "Metric dependency_request_seconds with wrong number of samples");

    sampleBucket.ifPresent(s -> {
      assertArrayEquals(tagValues, s.labelValues.toArray(), "Tags with wrong values");
      assertArrayEquals(tagNames, s.labelNames.toArray(), "Tags with wrong names");
    });

    var bucketSamples = samples.stream()
        .filter(sample -> "dependency_request_seconds_bucket".equals(sample.name)).collect(
            Collectors.toList());

    assertEquals(buckets.size() + 1, bucketSamples.size(),
        "Metric dependency_request_seconds with wrong number of samples");

    var sampleCount = samples.stream()
        .filter(sample -> "dependency_request_seconds_count".equals(sample.name)).findFirst();

    assertTrue(sampleCount.isPresent(),
        "Metric sample for dependency_request_seconds_count not found.");
    sampleCount.ifPresent(
        s -> assertEquals(1, s.value, "Metric dependency_request_seconds_count with wrong value"));

    var sampleSum = samples.stream()
        .filter(sample -> "dependency_request_seconds_sum".equals(sample.name))
        .collect(Collectors.toList());

    assertEquals(1d, sampleSum.size(),
        "Metric sample for dependency_request_seconds_sum not found.");
    assertTrue(sampleSum.get(0).value > 0d,
        "Metric dependency_request_seconds_sum with wrong value");

    restClient.simple(200);
    samples = Metrics.dependencyRequestSeconds.collect().get(0).samples;

    sampleCount = samples.stream()
        .filter(sample -> "dependency_request_seconds_count".equals(sample.name)).findFirst();

    assertTrue(sampleCount.isPresent(),
        "Metric sample for dependency_request_seconds_count not found.");

    sampleCount.
        ifPresent(s -> assertEquals(2d, s.value,
            "Metric dependency_request_seconds_count with wrong value"));
  }

  @Test
  void testCreatingRequestMetricsWithTagErrorInHeader() {
    var msgError = "error message example";
    try {
      restClient.simpleHeader(400, msgError);
    } catch (WebApplicationException e) {
      assertEquals(msgError, e.getResponse().getHeaderString(errorKey));
    }

    var sample = Metrics.dependencyRequestSeconds.collect().get(0).samples.get(0);

    assertEquals("true", sample.labelValues.get(5), "Receive wrong value for Tag isError");
    assertEquals(msgError, sample.labelValues.get(6),
        "Receive wrong value for Tag error message");
  }

  @Test
  void testCreatingRequestMetricsWithTagErrorInContainer() {
    var msgError = "error with describe in container";
    try {
      restClient.simpleContainer(400, msgError);
    } catch (WebApplicationException e) {
      assertEquals(400, e.getResponse().getStatus());
    }

    var sample = Metrics.dependencyRequestSeconds.collect().get(0).samples.get(0);

    assertEquals("true", sample.labelValues.get(5), "Receive wrong value for Tag isError");
    assertEquals(msgError, sample.labelValues.get(6),
        "Receive wrong value for Tag error message");
  }


  @Test
  void testCreatingOfDependencyRequestMetricByMonitorMetrics() {
    var tagValues = new String[]{"myDependency", "other", "OK", "GET", "myAddress", "true",
        "my error message", buckets.get(0)};
    var tagNames = new String[]{"name", "type", "status", "method", "addr", "isError",
        "errorMessage",
        "le"};

    DependencyEvent dependencyEvent = new DependencyEvent("myDependency")
        .setType("other")
        .setStatus("OK")
        .setMethod("GET")
        .setAddress("myAddress")
        .setIsError(true)
        .setErrorMessage("my error message");

    MonitorMetrics.INSTANCE.addDependencyEvent(dependencyEvent, 1d);

    var samples = Metrics.dependencyRequestSeconds.collect().get(0).samples;

    assertEquals(samples.size(),
        buckets.size() + 3, "Metric dependency_request_seconds with wrong number of samples");

    var sampleBucket = samples.stream()
        .filter(sample -> "dependency_request_seconds_bucket".equals(sample.name)).findFirst();

    assertTrue(sampleBucket.isPresent(),
        "Metric dependency_request_seconds with wrong number of samples");

    sampleBucket.ifPresent(s -> {
      assertArrayEquals(tagValues, s.labelValues.toArray(), "Tags with wrong values");
      assertArrayEquals(tagNames, s.labelNames.toArray(), "Tags with wrong names");
    });

    var bucketSamples = samples.stream()
        .filter(sample -> "dependency_request_seconds_bucket".equals(sample.name)).collect(
            Collectors.toList());

    assertEquals(buckets.size() + 1, bucketSamples.size(),
        "Metric dependency_request_seconds with wrong number of samples");

    var sampleCount = samples.stream()
        .filter(sample -> "dependency_request_seconds_count".equals(sample.name)).findFirst();

    assertTrue(sampleCount.isPresent(),
        "Metric sample for dependency_request_seconds_count not found.");
    sampleCount.ifPresent(
        s -> assertEquals(1, s.value, "Metric dependency_request_seconds_count with wrong value"));

    var sampleSum = samples.stream()
        .filter(sample -> "dependency_request_seconds_sum".equals(sample.name))
        .collect(Collectors.toList());

    assertEquals(1d, sampleSum.size(),
        "Metric sample for dependency_request_seconds_sum not found.");
    assertTrue(sampleSum.get(0).value > 0d,
        "Metric dependency_request_seconds_sum with wrong value");
  }

}
