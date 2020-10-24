package br.com.rubim.test.metrics;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.rubim.runtime.MonitorMetrics;
import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.runtime.request.RequestEvent;
import br.com.rubim.test.fake.filters.MetricsFilterForError;
import br.com.rubim.test.fake.resources.RequestResource;
import io.quarkus.test.QuarkusUnitTest;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class RequestMetricsTest {

  private static final String SIMPLE_PATH = "/request/simple";
  private static final String ERROR_PATH = "/request/with-error/";
  private static final String EXCLUSION_BASE_PATH = "/request/metric-exclusion-";

  @RegisterExtension
  static QuarkusUnitTest test = new QuarkusUnitTest()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
          .addClasses(RequestResource.class, MetricsFilterForError.class)
          .addAsResource(
              new StringAsset(
                  "quarkus.b5.monitor.exclusions=/request/metric-exclusion-one,/request/metric-exclusion-two,/request/metric-exclusion-two/{id}\n"
                      + "quarkus.b5.monitor.buckets=0.05,0.1,0.2,0.3,0.5,1.0,1.5,2,3,5,10.5\n"),
              "application.properties")
      );

  @ConfigProperty(name = "quarkus.b5.monitor.buckets")
  List<String> buckets;

  @ConfigProperty(name = "quarkus.b5.monitor.error-message")
  String errorKey;

  @BeforeEach
  void cleanMetrics() {
    Metrics.requestSeconds.clear();
  }

  @Test
  void testCreatingRequestMetrics() {
    var tagValues = new String[]{"http", "200", "GET", SIMPLE_PATH, "false", "", buckets.get(0)};
    var tagNames = new String[]{"type", "status", "method", "addr", "isError", "errorMessage",
        "le"};

    when().get(SIMPLE_PATH).then().statusCode(200);
    var samples = Metrics.requestSeconds.collect().get(0).samples;

    assertEquals(
        buckets.size() + 3, samples.size(), "Metric with wrong number of samples");

    var sampleBucket = samples.stream()
        .filter(sample -> "request_seconds_bucket".equals(sample.name)).findFirst();

    assertTrue(sampleBucket.isPresent(), "Metric sample for request_seconds_bucket not found.");

    sampleBucket.ifPresent(s -> {
      assertArrayEquals(tagValues, s.labelValues.toArray(), "Tags with wrong values");
      assertArrayEquals(tagNames, s.labelNames.toArray(), "Tags with wrong names");
    });

    var countBuckets = samples.stream()
        .filter(sample -> "request_seconds_bucket".equals(sample.name)).count();

    var sampleCount = samples.stream()
        .filter(sample -> "request_seconds_count".equals(sample.name)).findFirst();

    assertTrue(sampleCount.isPresent(), "Metric sample for request_seconds_count not found.");
    sampleCount
        .ifPresent(s -> assertEquals(1, s.value, "Metric request_seconds_count with wrong value"));

    var sampleSum = samples.stream()
        .filter(sample -> "request_seconds_sum".equals(sample.name)).findFirst();

    assertTrue(sampleSum.isPresent(), "Metric sample for request_seconds_sum not found.");
    sampleSum
        .ifPresent(s -> assertTrue(s.value > 0d, "Metric request_seconds_sum with wrong value"));

    assertEquals(buckets.size() + 1, countBuckets, "Metric with the wrong number of buckets");

    when().get(SIMPLE_PATH).then().statusCode(200);

    Metrics.requestSeconds.collect().get(0).samples.stream()
        .filter(sample -> "request_seconds_count".equals(sample.name)).findFirst()
        .ifPresent(s -> assertEquals(2, s.value, "Metric request_seconds_count with wrong value"));
  }

  @Test
  void testCreatingRequestMetricsWithTagErrorInHeader() {
    when().get(ERROR_PATH + "header/400" + "/" + errorKey).then().statusCode(400);

    var sample = Metrics.requestSeconds.collect().get(0).samples.get(0);

    assertEquals("true", sample.labelValues.get(4), "Receive wrong value for Tag isError");
    assertEquals("error with describe in header", sample.labelValues.get(5),
        "Receive wrong value for Tag error message");
  }

  @Test
  void testCreatingRequestMetricsWithTagErrorInContainer() {
    when().get(ERROR_PATH + "container/400").then().statusCode(400);

    var sample = Metrics.requestSeconds.collect().get(0).samples.get(0);

    assertEquals("true", sample.labelValues.get(4), "Receive wrong value for Tag isError");
    assertEquals("error with describe in container", sample.labelValues.get(5),
        "Receive wrong value for Tag error message");
  }

  @Test
  void testCreatingRequestMetricsExclusions() {
    when().get(EXCLUSION_BASE_PATH + "one").then().statusCode(200);
    assertEquals(0, Metrics.requestSeconds.collect().get(0).samples.size());

    when().get(EXCLUSION_BASE_PATH + "two").then().statusCode(200);
    assertEquals(0, Metrics.requestSeconds.collect().get(0).samples.size());

    when().get(EXCLUSION_BASE_PATH + "two/1").then().statusCode(200);
    assertEquals(0, Metrics.requestSeconds.collect().get(0).samples.size());
  }


  @Test
  void testCreatingRequestMetricsByMonitorMetrics() {
    var tagValues = new String[]{"other", "OK", "GET", "myAddress", "true", "my error message",
        buckets.get(0)};
    var tagNames = new String[]{"type", "status", "method", "addr", "isError", "errorMessage",
        "le"};

    RequestEvent requestEvent = new RequestEvent()
        .setType("other")
        .setStatus("OK")
        .setMethod("GET")
        .setAddress("myAddress")
        .setIsError(true)
        .setErrorMessage("my error message");

    MonitorMetrics.INSTANCE.addRequestEvent(requestEvent, 1d);

    var samples = Metrics.requestSeconds.collect().get(0).samples;

    assertEquals(
        buckets.size() + 3, samples.size(), "Metric with wrong number of samples");

    var sampleBucket = samples.stream()
        .filter(sample -> "request_seconds_bucket".equals(sample.name)).findFirst();

    assertTrue(sampleBucket.isPresent(), "Metric sample for request_seconds_bucket not found.");

    sampleBucket.ifPresent(s -> {
      assertArrayEquals(tagValues, s.labelValues.toArray(), "Tags with wrong values");
      assertArrayEquals(tagNames, s.labelNames.toArray(), "Tags with wrong names");
    });

    var countBuckets = samples.stream()
        .filter(sample -> "request_seconds_bucket".equals(sample.name)).count();

    var sampleCount = samples.stream()
        .filter(sample -> "request_seconds_count".equals(sample.name)).findFirst();

    assertTrue(sampleCount.isPresent(), "Metric sample for request_seconds_count not found.");
    sampleCount
        .ifPresent(s -> assertEquals(1, s.value, "Metric request_seconds_count with wrong value"));

    var sampleSum = samples.stream()
        .filter(sample -> "request_seconds_sum".equals(sample.name)).findFirst();

    assertTrue(sampleSum.isPresent(), "Metric sample for request_seconds_sum not found.");
    sampleSum
        .ifPresent(s -> assertTrue(s.value > 0d, "Metric request_seconds_sum with wrong value"));

    assertEquals(buckets.size() + 1, countBuckets, "Metric with the wrong number of buckets");

  }

}
