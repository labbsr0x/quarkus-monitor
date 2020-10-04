package br.com.rubim.test.metrics;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.test.RequestResource;
import io.quarkus.test.QuarkusUnitTest;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RequestMetricsTest {

  private static final String SIMPLE_PATH = "/request/simple";

  @RegisterExtension
  static QuarkusUnitTest test = new QuarkusUnitTest()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
              .addClasses(RequestResource.class)
              .addAsResource("application-um.properties", "application.properties")
          //.addAsResource(new StringAsset("quarkus.b5.monitor.exclusions=/metric-exclusion-one,/metric-exclusion-two/{id}"),"application.properties")
      );
  @ConfigProperty(name = "quarkus.b5.monitor.buckets")
  List<String> buckets;

  @Test
  public void testCreatingRequestMetrics() {
    var tagValues = new String[]{"http", "200", "GET", SIMPLE_PATH, "false", "", buckets.get(0)};
    var tagNames = new String[]{"type", "status", "method", "addr", "isError", "errorMessage",
        "le"};

    when().get(SIMPLE_PATH).then().statusCode(200);
    var samples = Metrics.requestSeconds.collect().get(0).samples;

    assertEquals(samples.size(),
        buckets.size() + 3, "Metric with wrong number of samples");

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
}
