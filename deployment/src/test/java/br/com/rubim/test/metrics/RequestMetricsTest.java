package br.com.rubim.test.metrics;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.rubim.runtime.MonitorMetrics;
import br.com.rubim.runtime.request.RequestEvent;
import br.com.rubim.test.fake.filters.MetricsFilterForError;
import br.com.rubim.test.fake.resources.RequestResource;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
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
  private static final String NAME = "request_seconds";

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
    Metrics.globalRegistry.clear();
  }

  @Test
  void testCreatingRequestMetrics() {
    var tagValues = new String[]{SIMPLE_PATH, "", "false", "GET", "200", "http"};
    var tagKeys = new String[]{"addr", "errorMessage", "isError", "method", "status", "type"};

    when().get(SIMPLE_PATH).then().statusCode(200);

    var samples = Metrics.globalRegistry.find(NAME).summaries();

    assertEquals(1, samples.size(),
        "Metric with wrong number of samples");

    var sample = samples.toArray(new DistributionSummary[0])[0];

    assertEquals(buckets.size(), sample.takeSnapshot().histogramCounts().length,
        "Metric with wrong number of buckets");

    var actualTagValues = sample.getId().getTags().stream().map(Tag::getValue)
        .toArray(String[]::new);
    var actualTagKeys = sample.getId().getTags().stream().map(Tag::getKey).toArray(String[]::new);

    assertArrayEquals(tagKeys, actualTagKeys, "Tags of " + NAME + " with wrong names");
    assertArrayEquals(tagValues, actualTagValues, "Tags of " + NAME + " with wrong values");

    assertTrue(sample.totalAmount() > 0, "Metric " + NAME + "_sum with wrong value");
    assertEquals(1, sample.count(), "Metric " + NAME + "_count with wrong value");

    when().get(SIMPLE_PATH).then().statusCode(200);
    assertEquals(2, sample.count(), "Metric " + NAME + "_count with wrong value");
  }

  @Test
  void testCreatingRequestMetricsWithTagErrorInHeader() {
    when().get(ERROR_PATH + "header/400" + "/" + errorKey).then().statusCode(400);

    var sample = Metrics.globalRegistry.find(NAME).summaries()
        .toArray(new DistributionSummary[0])[0];

    assertEquals("true", sample.getId().getTag("isError"),
        "Receive wrong value for Tag isError");
    assertEquals("error with describe in header", sample.getId().getTag("errorMessage"),
        "Receive wrong value for Tag error message");
  }

  @Test
  void testCreatingRequestMetricsWithTagErrorInContainer() {
    when().get(ERROR_PATH + "container/400").then().statusCode(400);

    var sample = Metrics.globalRegistry.find(NAME).summaries()
        .toArray(new DistributionSummary[0])[0];

    assertEquals("true", sample.getId().getTag("isError"), "Receive wrong value for Tag isError");
    assertEquals("error with describe in container", sample.getId().getTag("errorMessage"),
        "Receive wrong value for Tag error message");
  }

  @Test
  void testCreatingRequestMetricsExclusions() {
    var samples = Metrics.globalRegistry.find(NAME).counters();

    when().get(EXCLUSION_BASE_PATH + "one").then().statusCode(200);
    assertEquals(0, samples.size());

    when().get(EXCLUSION_BASE_PATH + "two").then().statusCode(200);
    assertEquals(0, samples.size());

    when().get(EXCLUSION_BASE_PATH + "two/1").then().statusCode(200);
    assertEquals(0, samples.size());
  }

  @Test
  void testCreatingRequestMetricsByMonitorMetrics() {
    var tagValues = new String[]{"myAddress", "my error message", "true", "GET", "OK", "other"};
    var tagKeys = new String[]{"addr", "errorMessage", "isError", "method", "status", "type"};

    RequestEvent requestEvent = new RequestEvent()
        .setType("other")
        .setStatus("OK")
        .setMethod("GET")
        .setAddress("myAddress")
        .setIsError(true)
        .setErrorMessage("my error message");

    MonitorMetrics.INSTANCE.addRequestEvent(requestEvent, 1d);

    var samples = Metrics.globalRegistry.find(NAME).summaries();

    assertEquals(1, samples.size(),
        "Metric with wrong number of samples");

    var sample = samples.toArray(new DistributionSummary[0])[0];

    assertEquals(buckets.size(), sample.takeSnapshot().histogramCounts().length,
        "Metric with wrong number of buckets");

    var actualTagValues = sample.getId().getTags().stream().map(Tag::getValue)
        .toArray(String[]::new);
    var actualTagKeys = sample.getId().getTags().stream().map(Tag::getKey).toArray(String[]::new);

    assertArrayEquals(tagKeys, actualTagKeys, "Tags of " + NAME + " with wrong names");
    assertArrayEquals(tagValues, actualTagValues, "Tags of " + NAME + " with wrong values");

    assertTrue(sample.totalAmount() > 0, "Metric request_seconds_sum with wrong value");
    assertEquals(1, sample.count(), "Metric request_seconds_count with wrong value");
  }

}
