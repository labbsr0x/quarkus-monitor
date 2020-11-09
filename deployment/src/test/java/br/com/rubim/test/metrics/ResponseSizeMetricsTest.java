package br.com.rubim.test.metrics;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.com.rubim.test.fake.filters.MetricsFilterForError;
import br.com.rubim.test.fake.resources.RequestResource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.quarkus.test.QuarkusUnitTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ResponseSizeMetricsTest {

  private static final String SIMPLE_PATH = "/request/simple";
  private static final String ERROR_PATH = "/request/with-error/";
  private static final String EXCLUSION_BASE_PATH = "/request/metric-exclusion-";
  private static final String NAME = "response_size_bytes";

  @RegisterExtension
  static QuarkusUnitTest test = new QuarkusUnitTest()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
          .addClasses(RequestResource.class, MetricsFilterForError.class)
          .addAsResource(
              new StringAsset(
                  "quarkus.b5.monitor.exclusions=/request/metric-exclusion-one,/request/metric-exclusion-two,/request/metric-exclusion-two/{id}\n"
                      + "quarkus.b5.monitor.enable-http-response-size=true\n"
              ),
              "application.properties")
      );

  @ConfigProperty(name = "quarkus.b5.monitor.error-message")
  String errorKey;

  @BeforeEach
  void cleanMetrics() {
    Metrics.globalRegistry.clear();
  }

  @Test
  void testCreatingResponseSizeBytesMetrics() {
    var tagValues = new String[]{SIMPLE_PATH, "", "false", "GET", "200", "http"};
    var tagKeys = new String[]{"addr", "errorMessage", "isError", "method", "status", "type"};

    when().get(SIMPLE_PATH).then().statusCode(200);

    assertNotNull(Metrics.globalRegistry.find(NAME).counter(), "Metric " + NAME + " not found");

    var samples = Metrics.globalRegistry.find(NAME).counters();

    assertEquals(1, samples.size(),
        "Metric response_size_bytes with wrong number of samples");

    var sample = samples.toArray(new Counter[0])[0];

    var actualTagValues = sample.getId().getTags().stream().map(Tag::getValue).toArray(
        String[]::new);
    var actualTagKeys = sample.getId().getTags().stream().map(Tag::getKey).toArray(String[]::new);

    assertArrayEquals(tagKeys, actualTagKeys, "Tags of response_size_bytes with wrong names");
    assertArrayEquals(tagValues, actualTagValues, "Tags of response_size_bytes with wrong values");

    assertEquals(2d, sample.count(), "Metric response_size_bytes with wrong value");

    when().get(SIMPLE_PATH).then().statusCode(200);
    assertEquals(4d, sample.count(), "Metric response_size_bytes with wrong value");
  }

  @Test
  void testCreatingResponseSizeBytesMetricsWithTagErrorInHeader() {
    when().get(ERROR_PATH + "header/400" + "/" + errorKey).then().statusCode(400);

    var sample = Metrics.globalRegistry.find(NAME).counters().toArray(new Counter[0])[0];

    assertEquals("true", sample.getId().getTag("isError"),
        "Wrong value for Tag isError from header in response_size_bytes metrics");
    assertEquals("error with describe in header", sample.getId().getTag("errorMessage"),
        "Wrong value for Tag errorMessage  from header in response_size_bytes metrics");
  }

  @Test
  void testCreatingResponseSizeBytesMetricsWithTagErrorInContainer() {
    when().get(ERROR_PATH + "container/400").then().statusCode(400);

    var sample = Metrics.globalRegistry.find(NAME).counters().toArray(new Counter[0])[0];

    assertEquals("true", sample.getId().getTag("isError"),
        "Wrong value for Tag isError from container in response_size_bytes metrics");
    assertEquals("error with describe in container", sample.getId().getTag("errorMessage"),
        "Wrong value for Tag errorMessage from container in response_size_bytes metrics");
  }

  @Test
  void testCreatingResponseSizeBytesMetricsExclusions() {
    when().get(EXCLUSION_BASE_PATH + "one").then().statusCode(200);

    var samples = Metrics.globalRegistry.find(NAME).counters();

    assertEquals(0, samples.size(),
        "Error in exclusion of path in response_size_bytes metrics with first path.");

    when().get(EXCLUSION_BASE_PATH + "two").then().statusCode(200);
    assertEquals(0, samples.size(),
        "Error in exclusion of path in response_size_bytes metrics with second path.");

    when().get(EXCLUSION_BASE_PATH + "two/1").then().statusCode(200);
    assertEquals(0, samples.size(),
        "Error in exclusion of path in response_size_bytes metrics with path param.");
  }
}